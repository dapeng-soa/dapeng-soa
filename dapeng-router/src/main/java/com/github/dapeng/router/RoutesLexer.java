package com.github.dapeng.router;

import com.github.dapeng.router.token.*;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 描述: 词法解析
 *
 * @author hz.lei
 * @date 2018年04月13日 下午9:04
 */
public class RoutesLexer {

    private String content;
    private int pos;

    //常量
    public static final String C_NOT = "~";
    public static final char C_MARKS = '\'';
    public static final char C_DOUBLE_MARKS = '\"';

    static SimpleToken Token_EOL = new SimpleToken(Token.EOL);
    static SimpleToken Token_THEN = new SimpleToken(Token.THEN);
    static SimpleToken Token_OTHERWISE = new SimpleToken(Token.OTHERWISE);
    static SimpleToken Token_MATCH = new SimpleToken(Token.MATCH);
    static SimpleToken Token_NOT = new SimpleToken(Token.NOT);
    static SimpleToken Token_EOF = new SimpleToken(Token.EOF);
    static SimpleToken Token_SEMI_COLON = new SimpleToken(Token.SEMI_COLON);
    static SimpleToken Token_COMMA = new SimpleToken(Token.COMMA);

    public static SimpleToken Token_MODE = new SimpleToken(Token.MODE);

    //    private static Pattern STRING_PATTERN = Pattern.compile("([\"\'])([a-zA-Z0-9*.]+)([\"\'])");

    private static Pattern rangerPattern = Pattern.compile("[0-9]+..[0-9]+");

    private static Pattern modePattern = Pattern.compile("%\"([0-9]+)n\\+(([0-9]+)..)?([0-9]+)\"");

    public RoutesLexer(String content) {
        this.content = content;
        this.pos = 0;
    }

    public Token peek() {
        int temPos = pos;
        Token token = next();
        pos = temPos;

        return token;
    }

    /**
     * 下一个token的id 必须为给定的type，不然报错
     * 回车符， 路由自然换行
     *
     * @param type
     */
    public Token peek(int type) {
        int temPos = pos;
        Token nextToken = next();
        pos = temPos;
        if (nextToken.id() != type) {
            throw new IllegalArgumentException("expect type: " + type + ", but got " + nextToken.id());
        }
        return nextToken;
    }


    public Token next() {
        if (pos == content.length()) {
            return Token_EOF;
        }
        char ch;
        do {
            if (pos == content.length()) {
                return Token_EOF;
            }

            ch = content.charAt(pos++);
            if (ch == '~') {
                return Token_NOT;
            }
            /*if (ch == '%') {
                return Token_MODE;
            }*/
            if (ch == '\n') {
                return Token_EOL;
            }
        }
        while (Character.isWhitespace(ch));

        StringBuilder sb = new StringBuilder();

        do {
            sb.append(ch);
            if (pos == content.length()) {
                break;
            }
            ch = content.charAt(pos++);
        } while (!Character.isWhitespace(ch));

        String token = sb.toString();

        if (token.equals("=>")) {
            return Token_THEN;
        }

        if (token.equals(";")) {
            return Token_SEMI_COLON;
        }

        if (token.equals("match")) {
            return Token_MATCH;
        }

        if (token.equals(",")) {
            return Token_COMMA;
        }

        if (token.equals("otherwise")) {
            return Token_OTHERWISE;
        }


        token.replaceAll("\"", "\'");
        if (token.length() > 1 && token.charAt(0) == C_MARKS) {
            if (token.charAt(token.length() - 1) == C_MARKS) {
                //去除引号
                String content = token.replaceAll("\'", "");
                if (content.contains("*")) {
                    return new RegexpToken(content);
                }
                return new StringToken(content);
            } else {
                throw new IllegalArgumentException("String字符串格式不正确...");
            }
        }


        if (token.length() >= 2 && token.substring(0, 2).equals("ip")) {
            String ips = token.substring(3, token.length() - 1);
            String[] split = ips.split("/");
            String ip = split[0];
            return new IpToken(ip, Integer.valueOf(split[1]));
        }
        if (rangerPattern.matcher(token).matches()) {
            int from = Integer.parseInt(token.substring(0, token.indexOf(".")));
            int to = Integer.parseInt(token.substring(token.lastIndexOf(".") + 1));
            return new RangeToken(from, to);
        }

        if (modePattern.matcher(token).matches()) {
            Matcher matcher = modePattern.matcher(token);
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

        //....
        peek(Token.MATCH);
        return new IdToken(token);

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


    public static void main(String[] args) {
        String property = System.getProperty("line.separator");
        System.out.println(property.length());

    }
}
