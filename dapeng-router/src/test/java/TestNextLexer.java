import com.github.dapeng.router.token.*;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 描述:
 *
 * @author hz.lei
 * @date 2018年04月17日 下午2:03
 */
public class TestNextLexer {

    private String content;
    private int pos;
    private static char[] keyWords = {',', ';', '\n'};

    static SimpleToken Token_EOL = new SimpleToken(Token.EOL);
    static SimpleToken Token_THEN = new SimpleToken(Token.THEN);
    static SimpleToken Token_OTHERWISE = new SimpleToken(Token.OTHERWISE);
    static SimpleToken Token_MATCH = new SimpleToken(Token.MATCH);
    static SimpleToken Token_NOT = new SimpleToken(Token.NOT);
    static SimpleToken Token_EOF = new SimpleToken(Token.EOF);
    static SimpleToken Token_SEMI_COLON = new SimpleToken(Token.SEMI_COLON);
    static SimpleToken Token_COMMA = new SimpleToken(Token.COMMA);


    private static Pattern ipPattern = Pattern.compile("ip(\"|\')([0-9.]+)(/[0-9]+)?(\"|\')");

    private static Pattern stringPattern = Pattern.compile("(\"|\')([A-Za-z0-9.*]+)(\"|\')");

    private static Pattern rangerPattern = Pattern.compile("([0-9]+)..([0-9]+)");

    private static Pattern modePattern = Pattern.compile("%\"([0-9]+)n\\+(([0-9]+)..)?([0-9]+)\"");


    public TestNextLexer(String content) {
        this.content = content;
        this.pos = 0;
    }

    public Token next() {
        // content length
        if (pos == content.length()) {
            return Token_EOF;
        }
        //get one
        char ch = content.charAt(pos);

        while (Character.isWhitespace(ch)) {
            if (pos == content.length()) {
                return Token_EOF;
            }
            pos++;
            ch = content.charAt(pos);
        }

        // 判断 ch 是否为 separators ？
        Token sepToken = matchSeparators(ch);
        if (sepToken != null) {
            return sepToken;
        }

        StringBuilder buffer = new StringBuilder();

        do {
            buffer.append(ch);
            pos++;
            ch = content.charAt(pos);
        } while (!breakCondition(ch));

        String value = buffer.toString();

        Token keyToken = matchKeyWords(value);

        return null;

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
                    return new IpToken(ip, 24);
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
        // id



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
            case '\n':
                return Token_EOL;
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
//                pos--;
                return true;
            }
        }
        return false;
    }


    public static void main(String[] args) {
        String content = "  \"methodFoo...***\", match \"setFoo.*\"";

        String ip1 = "  ip\"192.168.1.101/22\",";
        String ip2 = "  ip\"192.168.1.102\",";

        TestNextLexer lexer = new TestNextLexer(ip2);
        lexer.next();
        lexer.next();
        lexer.next();
    }

}
