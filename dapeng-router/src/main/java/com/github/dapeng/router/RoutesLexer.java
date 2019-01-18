package com.github.dapeng.router;

import com.github.dapeng.core.helper.IPUtils;
import com.github.dapeng.router.exception.ParsingException;
import com.github.dapeng.router.token.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.dapeng.router.token.Token.EOF;

/**
 * 描述: 路由规则 词法解析器
 *
 * @author hz.lei
 * @date 2018年04月13日 下午9:04
 */
public class RoutesLexer {
    /**
     * 文件结束符
     */
    private static final char EOI = '\uFFFF';

    private static Logger logger = LoggerFactory.getLogger(RoutesLexer.class);

    /**
     * 已经给trim处理过
     */
    private String content;
    private int pos;

    public static SimpleToken Token_EOL = new SimpleToken(Token.EOL);
    public static SimpleToken Token_THEN = new SimpleToken(Token.THEN);
    public static SimpleToken Token_OTHERWISE = new SimpleToken(Token.OTHERWISE);
    public static SimpleToken Token_MATCH = new SimpleToken(Token.MATCH);
    public static SimpleToken Token_NOT = new SimpleToken(Token.NOT);
    public static SimpleToken Token_EOF = new SimpleToken(EOF);
    public static SimpleToken Token_SEMI_COLON = new SimpleToken(Token.SEMI_COLON);
    public static SimpleToken Token_COMMA = new SimpleToken(Token.COMMA);


    /**
     * IP_REGEX
     * 暂时只支持全格式IP(加掩码), 对于192.168.10/24这种不支持
     */
    private static final String IP_REGEX = "(^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])\\.(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
            + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\.(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d))(/(\\d{2}))?(:(\\d{2,5}))?$";

    private static final Pattern IP_PATTERN = Pattern.compile(IP_REGEX);


    /**
     * 求模正则, 例如 1024n+0-8
     */
    private static final Pattern MODE_PATTERN = Pattern.compile("([0-9]+)n\\+(([0-9]+)..)?([0-9]+)");


    public RoutesLexer(String content) {
        this.content = content;
        this.pos = -1;
    }

    /**
     * 拿下一个 peek ，不改变偏移量。
     * <p>
     * todo 优化： 做一下缓存。不需要再次执行 next（）,方案？
     *
     * @return
     */
    public Token peek() throws ParsingException {
        int temPos = pos;
        Token token = next();
        pos = temPos;

        return token;
    }


    /**
     * 拿下一个 peek ，判断是不是 需要的类型。
     * <p>
     * todo 优化： 做一下缓存。不需要再次执行 next（）,方案？
     *
     * @return
     */
    public boolean ifPeek(int type) throws ParsingException {
        int temPos = pos;
        Token token = next();
        pos = temPos;

        return type == token.type();
    }


    /**
     * 获取下一个token 并改变 偏移量
     *
     * @return
     */
    public Token next() throws ParsingException {
        ws();
        char ch = nextChar();
        switch (ch) {
            case EOI:
                return Token_EOF;
            case '\r':
                require(new char[]{'\n'}, false);
            case '\n':
                return Token_EOL;
            case ',':
                return Token_COMMA;
            case ';':
                return Token_SEMI_COLON;
            case '~':
                return Token_NOT;
            case '%':
                require(new char[]{'\"', '\''}, true);
                return parserMode();
            case 'r':
                if (require(new char[]{'\"', '\''}, false)) {
                    return parserRegex();
                } else {
                    pos--;
                    return processId();
                }
            case '\"':
            case '\'':
                return parserString();
            case '=':
                require(new char[]{'>'}, true);
                return Token_THEN;
            case 'i':
                if (require(new char[]{'p'}, false)) {
                    return processIp();
                } else {
                    pos--;
                    return processId();
                }
                //process cookies
            case 'c':
                if (require(new char[]{'\"', '\''}, false)) {
                    return parseCookies();
                } else {
                    pos--;
                    return processId();
                }

                //process version
            case 'v':
                if (require(new char[]{'\"', '\''}, false)) {
                    return parseVersion();
                } else {
                    pos--;
                    return processId();
                }
            case '-':
            case '+':
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                return parserNumber();
            default:
                return processId();
        }
    }

    /**
     * 下一个token的id 必须为给定的type，不然报错
     * 回车符， 路由自然换行
     *
     * @param type
     */
    public Token next(int type) throws ParsingException {
        Token nextToken = next();
        throwExWithCondition(nextToken.type() != type,
                "[Not expected token]",
                "Expect:" + type + ", actually:" + nextToken.type());
        return nextToken;
    }

    /**
     * 解析 正则 token
     * <p>
     * r"get.*"
     * r'get.*'
     *
     * @return
     */
    private Token parserRegex() throws ParsingException {
        char quotation = currentChar();
        char ch = nextChar();
        StringBuilder sb = new StringBuilder(16);
        do {
            throwExWithCondition(ch == EOI,
                    "[RegexEx]", "parse IP_REGEX failed,check the IP_REGEX express:" + sb.toString());
            sb.append(ch);
        } while ((ch = nextChar()) != quotation);
        String value = sb.toString();
        return new RegexToken(value);
    }

    /**
     * c"storeId#11928654"
     *
     * @return
     */
    private Token parseCookies() throws ParsingException {
        char quotation = currentChar();
        char ch = nextChar();
        StringBuilder sb = new StringBuilder(16);
        do {
            throwExWithCondition(ch == EOI,
                    "[CookiesEx]", "parse COOKIE_RULES failed,check the COOKIE_REGEX express:" + sb.toString());
            sb.append(ch);
        } while ((ch = nextChar()) != quotation);
        String value = sb.toString();

        int pos = value.indexOf("#");

        if (pos != -1) {
            String backValue = value.substring(pos + 1);
            if (!backValue.contains("#")) {
                return new CookieToken(value.substring(0, pos), backValue);
            }
        }
        throw new ParsingException("[CookiesEx]", "parse COOKIE_RULES failed,check the cookie value contains '#' or more than one ");
    }

    /**
     * v"2.0.1"
     *
     * @return
     */
    private Token parseVersion() throws ParsingException {
        char quotation = currentChar();
        char ch = nextChar();
        StringBuilder sb = new StringBuilder(16);
        do {
            throwExWithCondition(ch == EOI,
                    "[VersionEx]", "parse VERSION_RULES failed,check the VERSION_REGEX express:" + sb.toString());
            sb.append(ch);
        } while ((ch = nextChar()) != quotation);
        String value = sb.toString();
        return new VersionToken(value);
    }


    /**
     * 解析 id
     *
     * @return
     */
    private Token processId() {
        StringBuilder sb = new StringBuilder(16);
        char ch = currentChar();
        do {
            sb.append(ch);
        } while (isValidIdChar(ch = nextChar()));
        pos--;

        String value = sb.toString();
        switch (value) {
            case "match":
                return Token_MATCH;
            case "otherwise":
                return Token_OTHERWISE;
            default:
                return new IdToken(value);
        }
    }

    /**
     * 解析 字母
     *
     * @param ch
     * @return
     */
    private boolean isValidIdChar(char ch) {
        return Character.isLetter(ch) || Character.isDigit(ch) || ch == '_' || ch == '.';
    }


    /**
     * parse string
     */
    private Token parserString() throws ParsingException {
        StringBuilder sb = new StringBuilder(16);
        char quotation = currentChar();
        char ch = nextChar();
        do {
            throwExWithCondition(ch == EOI,
                    "[StringEx]", "parse string failed,check the string express:" + sb.toString());
            sb.append(ch);
        } while ((ch = nextChar()) != quotation);

        return new StringToken(sb.toString());
    }

    /**
     * 解析整数常量
     */
    private Token parserNumber() throws ParsingException {
        StringBuilder sb = new StringBuilder(16);
        char ch = currentChar();
        do {
            sb.append(ch);
        } while (Character.isDigit(ch = nextChar()) || ch == '.');
        // added by Ever @20181207
        pos--;
        String value = sb.toString();
        try {
            if (value.contains("..")) {
                String[] nums = value.split("\\..");
                long from = Long.parseLong(nums[0]);
                long to = Long.parseLong(nums[1]);
                return new RangeToken(from, to);
            } else {
                int result = Integer.parseInt(sb.toString());
                return new NumberToken(result);
            }
        } catch (NumberFormatException e) {
            throw new ParsingException("[NumberEx]", "parse integer digit failed ,check the number express");
        }
    }


    /**
     * parserMode
     * <p>
     * 解析取模
     *
     * @return
     */
    private ModeToken parserMode() throws ParsingException {
        char quotation = currentChar();
        char ch = nextChar();
        StringBuilder sb = new StringBuilder(16);

        do {
            throwExWithCondition(ch == EOI,
                    "[ModeEx]", "parse mode failed,check the mode express:" + sb.toString());
            sb.append(ch);
        } while ((ch = nextChar()) != quotation);
        String value = sb.toString();

        try {
            Matcher matcher = MODE_PATTERN.matcher(value);
            if (matcher.matches()) {
                Long base = Long.parseLong(matcher.group(1));
                if (base == 0) {
                    throw new ParsingException("[ByZeroEx]", "取模被除数不能为0");
                }
                Optional<String> from = Optional.ofNullable(matcher.group(3));
                String to = matcher.group(4);

                return new ModeToken(base, from.map(Long::valueOf), Long.parseLong(to));
            }
        } catch (Throwable e) {
            throw new ParsingException("[ModeEx]", "mode IP_REGEX parse failed , check the mode expression again:" + value);
        }

        throw new ParsingException("[ModeEx]", "unknown exception, check the mode expression again:" + value);
    }

    private IpToken processIp() throws ParsingException {
        char quotation = nextChar();
        char ch = nextChar();

        StringBuilder sb = new StringBuilder(16);
        do {
            throwExWithCondition(ch == EOI,
                    "[IpEx]", "parse ip failed,check the ip express:" + sb.toString());
            sb.append(ch);
        } while ((ch = nextChar()) != quotation);
        Matcher matcher = IP_PATTERN.matcher(sb.toString());
        if (matcher.matches()) {
            String ipStr = matcher.group(1);
            int ip = IPUtils.transferIp(ipStr);

            // parse ip mask
            String maskStr = matcher.group(7);
            int mask = maskStr != null ? Integer.parseInt(maskStr) : IpToken.DEFAULT_MASK;

            // parse ip port
            String portStr = matcher.group(9);
            int port = portStr != null ? Integer.parseInt(portStr) : IpToken.DEFAULT_PORT;

            return new IpToken(ip, port, mask);
        }
        throw new ParsingException("[IpEx]", "parse ip failed,check the ip express");
    }


    private void throwExWithCondition(boolean condition, String summary, String detail) throws ParsingException {
        if (condition) {
            throw new ParsingException(summary, detail);
        }
    }

    /**
     * nextChar ()
     */
    private char nextChar() {
        return ++pos < content.length() ? content.charAt(pos) : EOI;
    }

    private char currentChar() {
        return pos < content.length() ? content.charAt(pos) : EOI;
    }


    /**
     * 跳过 white space  但是不包括 \n 回车换行符
     */
    private void ws() {
        char ws;
        while (((1L << (ws = nextChar())) & ((ws - 64) >> 31) & 0x100002600L) != 0L
                && ws != '\n'
                && ws != '\r') ;
        pos--;
    }


    /**
     * require next char or throw ex
     *
     * @param expects expects char []
     * @param isThrow if or not throw ex
     * @return {@code true } or {@code false}
     */
    private boolean require(char[] expects, boolean isThrow) throws ParsingException {
        char actual = nextChar();
        for (char expect : expects) {
            if (expect == actual) {
                return true;
            }
        }
        throwExWithCondition(isThrow,
                "[RequireEx]", "require char: " + Arrays.toString(expects) + " but actual char: " + actual);
        logger.debug("require char: " + Arrays.toString(expects) + " but actual char: " + actual);
        return false;
    }
}
