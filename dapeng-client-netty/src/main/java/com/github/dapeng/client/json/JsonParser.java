package com.github.dapeng.client.json;

import com.github.dapeng.org.apache.thrift.TException;

import java.util.Arrays;
import java.util.List;

public class JsonParser {

    public static final char EOI = '\uFFFF';
    //public static final char EOS = '\uFFFE';

    final ParserInput input;
    final JsonCallback callback;

    private char cursorChar;
    private StringBuilder sb = new StringBuilder(64);

    public JsonParser(String json, JsonCallback callback) {
        this.input = new StringBasedParserInput(json);
        this.callback = callback;

        cursorChar = input.nextChar();
    }

    public static class Line {
        public final int lineNr;
        public final int column;
        public final String text;


        public Line(int lineNr, int column, String text) {
            this.lineNr = lineNr;
            this.column = column;
            this.text = text;
        }

        @Override
        public String toString() {
            return "\"" + text + "\"," + "line:" + lineNr + ",column:" + column;
        }
    }

    public static class ParsingException extends RuntimeException {
        private final String summary;
        private final String detail;

        public ParsingException(String summary, String detail) {
            super(summary + ":" + detail);
            this.summary = summary;
            this.detail = detail;
        }
    }

    public static interface ParserInput {
        char nextChar();

        char nextUtf8Char();

        int cursor();

        char[] sliceCharArray(int start, int end);

        Line getLine(int Index);
    }

    static abstract class DefaultParserInput implements ParserInput {

        protected int _cursor = -1;

        public int cursor() {
            return _cursor;
        }
        //public void setCursor(int pos) { this._cursor = pos; }

        public Line getLine(int index) {
            StringBuilder sb = new StringBuilder();
            int savedCursor = _cursor;
            _cursor = -1;
            Line line = loop(index);
            _cursor = savedCursor;
            return line;
        }

        // TODO rewrite as while
        private Line rec(StringBuilder sb, int index, int ix, int lineStartIx, int lineNo) {
            char nc = nextUtf8Char();
            switch (nc) {
                case '\n':
                    if (index > ix) {
                        sb.setLength(0);
                        return rec(sb, index, ix + 1, ix + 1, lineNo + 1);
                    }
                case EOI:
                    return new Line(lineNo, index - lineStartIx + 1, sb.toString());
                default:
                    sb.append(nc);
                    return rec(sb, index, ix + 1, lineStartIx, lineNo);
            }
        }

        private Line loop(int index) {
            StringBuilder sb = new StringBuilder();
            int lineNo = 1;
            int ix = 0;
            int lineStartIx = 0;
            while (true) {
                char nc = nextUtf8Char();
                switch (nc) {
                    case '\n':
                        if (index > ix) {
                            sb.setLength(0);
                            lineNo++;
                            ix++;
                            lineStartIx = ix + 1;
                            break;
                        }
                    case EOI:
                        return new Line(lineNo, index - lineStartIx + 1, sb.toString());
                    default:
                        sb.append(nc);
                        ix++;
                }
            }
        }

    }

    public static class StringBasedParserInput extends DefaultParserInput {
        private final String string;

        public StringBasedParserInput(String string) {
            this.string = string;
        }

        @Override
        public char nextChar() {
            _cursor += 1;
            if (_cursor < string.length())
                return string.charAt(_cursor);
            else return EOI;
        }

        @Override
        public char nextUtf8Char() {
            return nextChar();
        }

        @Override
        public char[] sliceCharArray(int start, int end) {
            char[] array = new char[end - start];
            string.getChars(start, end, array, 0);
            return array;
        }
    }

    ParsingException fail(String target) {
        return fail(target, input.cursor(), cursorChar);
    }

    ParsingException fail(String target, int cursor) {
        return fail(target, cursor, cursorChar);
    }

    ParsingException fail(String target, int cursor, char errorChar) {
        Line line = input.getLine(cursor);

        String unexpected = null;
        if (errorChar == EOI) unexpected = "end-of-input";
        else if (Character.isISOControl(errorChar)) unexpected = String.format("\\u%04x", (int) errorChar);
        else unexpected = "" + errorChar;

        String expected = (target.equals("'\uFFFF'")) ? "end-of-input" : target;

        String summary = "Unexpected " + unexpected + " at input index:" +
                input.cursor() + "(line:" + line.lineNr + ",position:" + line.column +
                "), expected: " + expected;

        String detail = line.text; // TODO

        return new ParsingException(summary, detail);
    }

    void parseJsValue() throws TException {
        ws();
        value();
        require(EOI);
    }

    void ws() {
        while (((1L << cursorChar) & ((cursorChar - 64) >> 31) & 0x100002600L) != 0L) {
            advance();
        }
    }

    boolean advance() {
        cursorChar = input.nextChar();
        return true;
    }

    void value() throws TException {
        int mark = input.cursor();

        switch (cursorChar) {
            case 'f':
                if (!_false()) throw fail("JSON Value", mark);
                callback.onBoolean(false);
                break;
            case 'n':
                if (!_null()) throw fail("JSON Value", mark);
                ;
                callback.onNull();
                break;
            case 't':
                if (!_true()) throw fail("JSON Value", mark);
                ;
                callback.onBoolean(true);
                break;
            case '{':
                advance();
                callback.onStartObject();
                object();
                callback.onEndObject();
                break;
            case '[':
                advance();
                callback.onStartArray();
                array();
                callback.onEndArray();
                break;
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
            case '-':
                number();
                //callback.onNumber();
                break;
            case '\"':
                string();
                callback.onString(sb.toString());
                break;
            default:
                throw fail("JSON Value");
        }

    }

    boolean _false() {
        advance();
        return ch('a') && ch('l') && ch('s') && ws('e');
    }

    boolean _null() {
        advance();
        return ch('u') && ch('l') && ws('l');
    }

    boolean _true() {
        advance();
        return ch('r') && ch('u') && ws('e');
    }

    boolean ch(char c) {
        if (cursorChar == c) {
            advance();
            return true;
        } else return false;
    }

    void require(char c) {
        if (!ch(c))
            throw fail("'" + c + "'");
    }

    boolean ws(char c) {
        if (ch(c)) {
            ws();
            return true;
        } else return false;
    }

    boolean _char() {
        if (((1L << cursorChar) & ((31 - cursorChar) >> 31) & 0x3ffffffbefffffffL) != 0L)
            return appendSB(cursorChar);
        else {
            switch (cursorChar) {
                case '\"':
                case EOI:
                    return false;
                case '\\':
                    advance();
                    return escaped();
                default:
                    return cursorChar >= ' ' && appendSB(cursorChar);
            }
        }
    }

    int hexValue(char c) {
        if ('0' <= c && c <= '9') return c - '0';
        else if ('a' <= c && c <= 'f') return c - 87;
        else if ('A' <= c && c <= 'F') return c = 55;
        else throw fail("hex digit");
    }

    boolean escaped() {
        switch (cursorChar) {
            case '"':
            case '/':
            case '\\':
            case '\'':
                return appendSB(cursorChar);
            case 'b':
                return appendSB('\b');
            case 'f':
                return appendSB('\f');
            case 'n':
                return appendSB('\n');
            case 'r':
                return appendSB('\r');
            case 't':
                return appendSB('\t');
            case 'u':
                advance();
                int value = hexValue(cursorChar);
                advance();
                value = (value << 4) + hexValue(cursorChar);
                advance();
                value = (value << 4) + hexValue(cursorChar);
                advance();
                value = (value << 4) + hexValue(cursorChar);
                return appendSB((char) value);
            default:
                throw fail("JSON escape sequencne");
        }
    }

    boolean appendSB(char c) {
        sb.append(c);
        return true;
    }

    void object() throws TException {
        ws();
        if (cursorChar != '}') {
            members();
        }
        require('}');
        ws();
    }

    void members() throws TException {
        do {
            string();
            require(':');
            ws();

            String key = sb.toString();
            callback.onStartField(key);

            value();

            callback.onEndField();

        } while (ws(','));
    }


    void string() {
        if (cursorChar == '"') cursorChar = input.nextUtf8Char();
        else throw fail("'\"'");

        sb.setLength(0);
        while (_char()) {
            cursorChar = input.nextUtf8Char();
        }
        require('\"');
        ws();
    }

    void array() throws TException {
        ws();
        if (cursorChar != ']') {
            do {
                value();
            } while (ws(','));
        }
        require(']');
        ws();
    }

    void number() throws TException {
        int start = input.cursor();
        int startChar = cursorChar;

        ch('-');
        _int();
        frac();
        exp();

        callback.onNumber(Double.parseDouble(new String(input.sliceCharArray(start, input.cursor()))));
        ws();
    }

    void _int() {
        if (!ch('0')) oneOrMoreDigits();
    }

    void frac() {
        if (ch('.')) oneOrMoreDigits();
    }

    void exp() {
        if (ch('e') || ch('E')) {
            ch('-');
            ch('+');
            oneOrMoreDigits();
        }
    }

    void oneOrMoreDigits() {
        if (digit()) zeroOrMoreDigits();
        else throw fail("DIGIT");
    }

    void zeroOrMoreDigits() {
        while (digit()) {
        }
    }

    boolean digit() {
        return cursorChar >= '0' && cursorChar <= '9' && advance();
    }


    public static void main(String[] args) throws TException {
        String json = "{ \"a\": 10, \"b\": true, \n\"c\": [1,2,3], \"d\":10.2," +
                "\"user\": { \"name\": \"wangzx\", \"age\": 10 }, \n\"emptyArray\":[],\"emptyObject\":{} }";

        String json1 = "{ a\": 10, \"b\": true, \n\"c\": [1,2,3], " +
                "\"user\": { \"name\": \"wangzx\", \"age\": 10 }, \n\"emptyArray\":[],\"emptyObject\":{} }";

        String json2 = "{ \"a\": 10d, \"b\": true, \n\"c\": [1,2,3], " +
                "\"user\": { \"name\": \"wangzx\", \"age\": 10 }, \n\"emptyArray\":[],\"emptyObject\":{} }";

        String json3 = "{ \"a\": 10, b\": true, \n\"c\": [1,2,3], " +
                "\"user\": { \"name\": \"wangzx\", \"age\": 10 }, \n\"emptyArray\":[],\"emptyObject\":{} }";

        String json4 = "{ \"a\": 10, \"b\": true, \n\"c: [1,2,3], " +
                "\"user\": { \"name\": \"wangzx\", \"age\": 10 }, \n\"emptyArray\":[],\"emptyObject\":{} }";
        String json5 = "{ \"a\": 10, \"b\": true, \n\"c\": 1,2,3], " +
                "\"user\": { \"name\": \"wangzx\", \"age\": 10 }, \n\"emptyArray\":[],\"emptyObject\":{} }";
        String json6 = "{ \"a\": 10, \"b\": true, \n\"c\": [1,2,3] " +
                "\"user\": { \"name\": \"wangzx\", \"age\": 10 }, \n\"emptyArray\":[],\"emptyObject\":{} }";
        String json7 = "{ \"a\": 10, \"b\": true, \n\"c\": [1,2,3, " +
                "\"user\": { \"name\": \"wangzx\", \"age\": 10 }, \n\"emptyArray\":[],\"emptyObject\":{} }";
        String json8 = "{ \"a\": 10, \"b\": true, \n\"c\": [1,2,3], " +
                "\"user\": { \"name\": \"wangzx\", \"age\": 10 }, \n\"emptyArray\":[],\"emptyObject\":} }";
        String json9 = "{ \"a\": 10, \"b\": true, \n\"c\": [1,2,3], " +
                "\"user\": { \"name\": \"wangzx\", \"age\": 10 }, \n\"emptyArray\":[],\"emptyObject\":{ }";
        String json10 = "{ \"a\": 10, \"b\": true, \n\"c\": [1,2,3], " +
                "\"user\": { \"name\": \"wangzx\", \"age\": 10 }, \n\"emptyArray\":[],\"emptyObject\":{}, }";

        List<String> errorJsons = Arrays.asList(json1, json2, json3, json4, json5, json6, json7, json8, json9, json10);
        JsonCallback callback = new JsonCallback() {
            @Override
            public void onStartObject() {
                System.out.println("onStartObject");
            }

            @Override
            public void onEndObject() {
                System.out.println("onEndObject");
            }

            @Override
            public void onStartArray() {
                System.out.println("onStartArray");
            }

            @Override
            public void onEndArray() {
                System.out.println("onEndArray");
            }

            @Override
            public void onStartField(String name) {
                System.out.println("onStartField:" + name);
            }

            @Override
            public void onEndField() {
                System.out.println("onEndField");
            }

            @Override
            public void onBoolean(boolean value) {
                System.out.println("onBoolean:" + value);
            }

            @Override
            public void onNumber(double value) {
                System.out.println("onNumber:" + value);
            }

            @Override
            public void onNull() {
                System.out.println("onNull");
            }

            @Override
            public void onString(String value) {
                System.out.println("onString:" + value);
            }
        };

        JsonParser parser = new JsonParser(json, callback);
        System.out.println(json);
        parser.value();
System.out.println("finished=====");
//        errorJsons.forEach(errorJson -> {
//            JsonParser myParser = new JsonParser(errorJson, callback);
//            try {
//                myParser.value();
//            } catch (ParsingException e) {
//                e.printStackTrace();
//            }
//            System.out.println("finished=====");
//        });
    }

}
