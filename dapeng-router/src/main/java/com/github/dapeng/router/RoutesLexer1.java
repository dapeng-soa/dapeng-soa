package com.github.dapeng.router;

import com.github.dapeng.router.token.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.dapeng.router.token.Token.EOF;

/**
 * 描述: 词法解析 -- 最新
 *
 * @author hz.lei
 * @date 2018年04月13日 下午9:04
 */
public class RoutesLexer1 {

    private static final char EOI = '\uFFFF';

    private static Logger logger = LoggerFactory.getLogger(RoutesLexer1.class);

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
     * regex fixme 更加严谨的正则
     */
    private static Pattern ipPattern = Pattern.compile("([0-9.]+)(/[0-9]+)?");

    private static Pattern modePattern = Pattern.compile("([0-9]+)n\\+(([0-9]+)..)?([0-9]+)");

    public RoutesLexer1(String content) {
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
            throw new IllegalArgumentException("");
        }
        return nextToken;
    }

    /**
     * exception
     */
    public static class ParsingException extends RuntimeException {
        private final String summary;
        private final String detail;

        public ParsingException(String summary, String detail) {
            super(summary + ":" + detail);
            this.summary = summary;
            this.detail = detail;
        }
    }

    /**
     * 解析 正则 token
     *
     * @return
     */
    private Token parserRegex() {
        char quotation = currentChar();
        char ch = nextChar();
        StringBuilder sb = new StringBuilder(16);
        do {
            sb.append(ch);
            ch = nextChar();
        }
        while (ch != quotation);
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
            ch = nextChar();
        } while (isLetterOrDigit(ch));
        pos--;

        String value = sb.toString();
        switch (value) {
            case "match":
                return Token_MATCH;
            case "otherwise":
                return Token_OTHERWISE;
            default:
                break;
        }
        return new IdToken(sb.toString());
    }

    /**
     * 解析 字母
     *
     * @param ch
     * @return
     */
    public boolean isLetterOrDigit(char ch) {
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
            sb.append(ch);
            ch = nextChar();
        }
        while (ch != quotation);
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
            ch = nextChar();
        }
        while (Character.isDigit(ch) || ch == '.');
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
            sb.append(ch);
            ch = nextChar();
        }
        while (ch != quotation);
        String value = sb.toString();

        try {
            if (modePattern.matcher(value).matches()) {
                Matcher matcher = modePattern.matcher(value);
                while (matcher.find()) {
                    String base = matcher.group(1);
                    String from = matcher.group(3);
                    String to = matcher.group(4);
                    if (from == null) {
                        return new ModeToken(Long.parseLong(base), Optional.empty(), Long.parseLong(to));
                    } else {
                        return new ModeToken(Long.parseLong(base), Optional.of(Long.parseLong(from)), Long.parseLong(to));
                    }
                }
            }
        } catch (Exception e) {
            throw new ParsingException("[ModeEx]", "mode regex parse failed , check the mode expression again");
        }
        throw new ParsingException("[ModeEx]", "mode regex parse failed , check the mode expression again");
    }

    private IpToken processIp() {
        char quotation = nextChar();
        char ch = nextChar();

        StringBuilder sb = new StringBuilder(16);
        do {
            sb.append(ch);
            ch = nextChar();
        }
        while (ch != quotation);

        Matcher matcher = ipPattern.matcher(sb.toString());
        while (matcher.find()) {
            String ipStr = matcher.group(1);
            int ip;
            ip = IPUtils.transferIp(ipStr);

            String masks = matcher.group(2);
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
        do {
            ws = nextChar();

        } while (((1L << ws) & ((ws - 64) >> 31) & 0x100002600L) != 0L && ws != '\n');
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



    /*public static void main(String[] args) {
        String context = "  method match \'大佬好\',\"setFoo\", 'getFoo' => ip\"192.168.1.123\" ";
//        String context = "  method match %\'1024n+8\'  ,\"setFoo\", 'getFoo' => ip\"192.168.1.123\" ";
//        String context = "  iUserId match r\"setFoo.*\" => ip\"192.168.1.123\" ";
        RoutesLexer1 lexer = new RoutesLexer1(context);
        IdToken token = (IdToken) lexer.next();

        int id = token.id();
        System.out.println(token.name);

        Token token1 = lexer.next();
        System.out.println(token1.id());

        Token token2 = lexer.next();
        System.out.println(token2.id());

        Token token3 = lexer.next();
        System.out.println(token3.id());

        Token token4 = lexer.next();
        System.out.println(token4.id());

        Token token5 = lexer.next();
        System.out.println(token5.id());

        Token token6 = lexer.next();
        System.out.println(token6.id());


    }*/

}
