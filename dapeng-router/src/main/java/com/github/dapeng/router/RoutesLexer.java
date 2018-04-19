package com.github.dapeng.router;

import com.github.dapeng.router.token.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static Logger logger = LoggerFactory.getLogger(RoutesLexer.class);

    private String content;
    private int pos;

    static SimpleToken Token_EOL = new SimpleToken(Token.EOL);
    static SimpleToken Token_THEN = new SimpleToken(Token.THEN);
    static SimpleToken Token_OTHERWISE = new SimpleToken(Token.OTHERWISE);
    static SimpleToken Token_MATCH = new SimpleToken(Token.MATCH);
    static SimpleToken Token_NOT = new SimpleToken(Token.NOT);
    static SimpleToken Token_EOF = new SimpleToken(Token.EOF);
    static SimpleToken Token_SEMI_COLON = new SimpleToken(Token.SEMI_COLON);
    static SimpleToken Token_COMMA = new SimpleToken(Token.COMMA);

    /**
     * regex
     */
    private static Pattern ipPattern = Pattern.compile("ip(\"|\')([0-9.]+)(/[0-9]+)?(\"|\')");

    private static Pattern stringPattern = Pattern.compile("(\"|\')([A-Za-z0-9.*]+)(\"|\')");

    private static Pattern rangerPattern = Pattern.compile("([0-9]+)..([0-9]+)");

    private static Pattern modePattern = Pattern.compile("%(\"|\')([0-9]+)n\\+(([0-9]+)..)?([0-9]+)(\"|\')");

    // name match "wang,zx"
    private static char[] keyWords = {',', ';', '\n','='};

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
        try {
            int temPos = pos;
            Token nextToken = next();
            pos = temPos;
            if (nextToken.id() != type) {
                throw new IllegalArgumentException("expect type: " + type + ", but got " + nextToken.id());
            }

            return nextToken;
        } catch (Exception e) {
            logger.error("parse peek error: " + e.getMessage(), e);
        }
        return null;
    }

    public Token next() {
        // content length
        if (pos == content.length()) {
            // 第一次 需要 return ，因为 是从 第一个字符开始解析的，如果判断第一个为EOF，即结束
            return Token_EOF;
        }
        //get one
        char ch = content.charAt(pos);

        if (ch == '\n') {
            pos++;
            return Token_EOL;
        }

        while (Character.isWhitespace(ch)) {
            pos++;
            if (ch == '\n') {
                return Token_EOL;
            }
            if (pos == content.length()) {
                return Token_EOF;
            }
            ch = content.charAt(pos);
        }
        // 判断 ch 是否为 separators ？
        Token sepToken = matchSeparators(ch);
        if (sepToken != null) {
            pos++;
            return sepToken;
        }
        StringBuilder buffer = new StringBuilder();
        do {
            buffer.append(ch);
            pos++;
            if (pos == content.length()) {
                //  第二次不能 return Token_EOF , 考虑如下情形："method" ，解析道了这句话末尾，应该拿到 method ,如果 return 将返回 EOF
                break;
            }
            ch = content.charAt(pos);
        } while (!breakCondition(ch));

        String value = buffer.toString();

        Token keyToken = matchKeyWords(value);

        return keyToken;

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
     * 关键字匹配
     *
     * @param value
     * @return
     */
    private Token matchKeyWords(String value) {
        switch (value) {
            case "=>":
                return Token_THEN;
            case "match":
                return Token_MATCH;
            case "otherwise":
                return Token_OTHERWISE;
            default:
                break;
        }

        // 字符串
        if (stringPattern.matcher(value).matches()) {
            Matcher matcher = stringPattern.matcher(value);
            while (matcher.find()) {
                String content = matcher.group(2);
                if (content.contains(".*")) {
                    return new RegexpToken(content);
                }
                return new StringToken(content);
            }
        }
        // ip
        if (ipPattern.matcher(value).matches()) {
            Matcher matcher = ipPattern.matcher(value);
            while (matcher.find()) {
                String ip = matcher.group(2);
                String masks = matcher.group(3);

                if (masks != null) {
                    int mask = Integer.parseInt(masks.substring(1));
                    return new IpToken(ip, mask);
                } else {
                    // 默认值，mask
                    return new IpToken(ip, 0);
                }
            }
        }
        // 范围
        if (rangerPattern.matcher(value).matches()) {
            Matcher matcher = rangerPattern.matcher(value);
            while (matcher.find()) {
                int from = Integer.parseInt(matcher.group(1));
                int to = Integer.parseInt(matcher.group(2));
                return new RangeToken(from, to);
            }
        }
        // 取模
        if (modePattern.matcher(value).matches()) {
            Matcher matcher = modePattern.matcher(value);
            while (matcher.find()) {
                String base = matcher.group(2);
                String from = matcher.group(4);
                String to = matcher.group(5);

                if (from == null) {
                    return new ModeToken(Long.parseLong(base), Optional.empty(), Long.parseLong(to));
                } else {
                    return new ModeToken(Long.parseLong(base), Optional.of(Long.parseLong(from)), Long.parseLong(to));
                }
            }
        }
        // id
        if (peek(Token.MATCH) != null) {
            return new IdToken(value);
        }

        logger.error("no suitable token found ");
        return null;
    }

    /**
     * 判断 是否为分隔符
     *
     * @param ch
     * @return
     */
    private Token matchSeparators(char ch) {
        switch (ch) {
            case ',':
                return Token_COMMA;
            case ';':
                return Token_SEMI_COLON;
            case '~':
                return Token_NOT;
            default:
                break;
        }
        return null;
    }

    private boolean breakCondition(char ch) {
        if (Character.isWhitespace(ch)) {
            return true;
        }
        for (char key : keyWords) {
            if (ch == key) {
                return true;
            }
        }
        return false;
    }


    //~~~~~~~～～～～～～～～～
    /*public Token nextOld() {
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
            *//*if (ch == '%') {
                return Token_MODE;
            }*//*
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
        } while (!breakCondition(ch));


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

    }*/


    /*public static void main(String[] args) {
        String property = System.getProperty("line.separator");
        System.out.println(property.length());

    }*/
}
