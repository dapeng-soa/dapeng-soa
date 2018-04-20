package com.github.dapeng.router;

import com.github.dapeng.core.helper.IPUtils;
import com.github.dapeng.router.exception.ParsingException;
import com.github.dapeng.router.token.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final char EOI = '\uFFFF';

    private static Logger logger = LoggerFactory.getLogger(RoutesLexer.class);

    private String content;
    private int pos;

    static SimpleToken Token_EOL = new SimpleToken(Token.EOL);
    static SimpleToken Token_THEN = new SimpleToken(Token.THEN);
    static SimpleToken Token_OTHERWISE = new SimpleToken(Token.OTHERWISE);
    static SimpleToken Token_MATCH = new SimpleToken(Token.MATCH);
    static SimpleToken Token_NOT = new SimpleToken(Token.NOT);
    static SimpleToken Token_EOF = new SimpleToken(EOF);
    static SimpleToken Token_SEMI_COLON = new SimpleToken(Token.SEMI_COLON);
    static SimpleToken Token_COMMA = new SimpleToken(Token.COMMA);

    /**
     * regex  严谨的ip正则
     */
    private static String regex = "(^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])\\.(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
            + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\.(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d))(/(\\d{2}))?$";

    private static Pattern ipPattern = Pattern.compile(regex);
    private static Pattern modePattern = Pattern.compile("([0-9]+)n\\+(([0-9]+)..)?([0-9]+)");

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
    public Token peek() {
        int temPos = pos;
        Token token = next();
        pos = temPos;

        return token;
    }

    /**
     * 获取下一个token 并改变 偏移量
     *
     * @return
     */
    public Token next() {
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
    public Token next(int type) {
        Token nextToken = next();
        if (nextToken.id() != type) {
            throw new ParsingException("[Not expected token]",
                    "Expect:" + type + ", actually:" + nextToken.id());
        }
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
    private Token parserRegex() {
        char quotation = currentChar();
        char ch = nextChar();
        StringBuilder sb = new StringBuilder(16);
        do {
            if (ch == EOI || ch == EOF) {
                throw new ParsingException("[RegexEx]", "parse regex failed,check the regex express:" + sb.toString());
            }
            sb.append(ch);
        } while ((ch = nextChar()) != quotation);
        String value = sb.toString();
        return new RegexpToken(value);
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
        } while (isLetterOrDigit(ch = nextChar()));
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
    private boolean isLetterOrDigit(char ch) {
        boolean letter = Character.isLetter(ch);
        boolean digit = Character.isDigit(ch);

        return letter || digit;
    }


    /**
     * parse string
     */
    private Token parserString() {
        StringBuilder sb = new StringBuilder(16);
        char quotation = currentChar();
        char ch = nextChar();
        do {
            if (ch == EOI || ch == EOF) {
                throw new ParsingException("[StringEx]", "parse string failed,check the string express:" + sb.toString());
            }
            sb.append(ch);
        } while ((ch = nextChar()) != quotation);
        return new StringToken(sb.toString());
    }

    /**
     * 解析整数常量
     */
    private Token parserNumber() {
        StringBuilder sb = new StringBuilder(16);
        char ch = currentChar();
        do {
            sb.append(ch);
        } while (Character.isDigit(ch = nextChar()) || ch == '.');
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
    private ModeToken parserMode() {
        char quotation = currentChar();
        char ch = nextChar();
        StringBuilder sb = new StringBuilder(16);

        do {
            if (ch == EOI || ch == EOF) {
                throw new ParsingException("[ModeEx]", "parse mode failed,check the mode express:" + sb.toString());
            }
            sb.append(ch);
        } while ((ch = nextChar()) != quotation);

        String value = sb.toString();

        try {
            Matcher matcher = modePattern.matcher(value);
            if (matcher.matches()) {
                String base = matcher.group(1);
                Optional<String> from = Optional.ofNullable(matcher.group(3));
                String to = matcher.group(4);

                return new ModeToken(Long.parseLong(base), from.map(Long::valueOf), Long.parseLong(to));
            }
        } catch (Throwable e) {
            throw new ParsingException("[ModeEx]", "mode regex parse failed , check the mode expression again:" + value);
        }

        throw new ParsingException("[ModeEx]", "unknown exception, check the mode expression again:" + value);
    }

    private IpToken processIp() {
        char quotation = nextChar();
        char ch = nextChar();

        StringBuilder sb = new StringBuilder(16);
        do {
            if (ch == EOI || ch == EOF) {
                throw new ParsingException("[IpEx]", "parse ip failed,check the ip express:" + sb.toString());
            }
            sb.append(ch);
        } while ((ch = nextChar()) != quotation);

        Matcher matcher = ipPattern.matcher(sb.toString());
        if (matcher.matches()) {
            String ipStr = matcher.group(1);
            int ip;
            ip = IPUtils.transferIp(ipStr);

            String masks = matcher.group(7);
            if (masks != null) {
                int mask = Integer.parseInt(masks.substring(1));
                return new IpToken(ip, mask);
            } else {
                // 默认值，mask
                return new IpToken(ip, 32);
            }
        }
        throw new ParsingException("[IpEx]", "parse ip failed,check the ip express");
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
     * @param expects
     * @param isThrow
     * @return
     */
    private boolean require(char[] expects, boolean isThrow) {
        char actual = nextChar();
        for (char expect : expects) {
            if (expect == actual) {
                return true;
            }
        }
        if (isThrow) {
            throw new ParsingException("[RequireEx]", "require char: " + expects.toString() + " but actual char: " + actual);
        }
        logger.debug("require char: " + expects.toString() + " but actual char: " + actual);
        return false;
    }
}
