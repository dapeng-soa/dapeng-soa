/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.dapeng.json;


/**
 * @author zxwang
 */
public final class JsonParser {

    private static final char EOI = '\uFFFF';

    final JsonCallback callback;

    // private final char[] chars;
    private final String chars;
    private final int length;
    private int cursor = -1;
    private char cursorChar;


    private char[] buffer = new char[1024];
    private int bufferLength = 0;

    // either fieldName or String
    private String stringValue;

    private void appendBuffer(char ch) {
        if(bufferLength + 1 < buffer.length) {
            buffer[bufferLength++] = ch;
        }
        else _appendBuffer2(ch);
    }

    private void _appendBuffer2(char ch) {
          char[] newBuffer = new char[buffer.length * 2];
          System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
          this.buffer = newBuffer;
          buffer[bufferLength++] = ch;
    }

    private void resetBuffer(){
        this.bufferLength = 0;
    }

    private String getBufferString() {
        return new String(buffer, 0, bufferLength);
    }


    public JsonParser(String json, JsonCallback callback) {
        this.callback = callback;

//        this.chars = json.toCharArray();
        this.chars = json;
        this.length = chars.length();

        cursorChar = nextChar();
    }

    private int cursor() {
        return cursor;
    }

    private Line getLine(int index) {
        int savedCursor = cursor;
        cursor = -1;
        Line line = loop(index);
        cursor = savedCursor;
        return line;
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

    private char charAt(int pos) {
        return this.chars.charAt(pos);
    }

    private char nextChar() {
        cursor += 1;
        if (cursor < length)
            return charAt(cursor);
        else return EOI;
    }

    private char nextUtf8Char() {
        return nextChar();
    }


    private char[] sliceCharArray(int start, int end) {
        char[] array = new char[end - start];
        for(int i = start; i < end; i++)
            array[i - start] = charAt(i);
        return array;
    }


    public static class Line {
        final int lineNr;
        final int column;
        public final String text;


        Line(int lineNr, int column, String text) {
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

        ParsingException(String summary, String detail) {
            super(summary + ":" + detail);
        }
    }



    ParsingException fail(String target) {
        return fail(target, cursor(), cursorChar);
    }

    ParsingException fail(String target, int cursor) {
        return fail(target, cursor, cursorChar);
    }

    ParsingException fail(String target, int cursor, char errorChar) {
        Line line = getLine(cursor);

        String unexpected;
        if (errorChar == EOI) unexpected = "end-of-input";
        else if (Character.isISOControl(errorChar)) unexpected = String.format("\\u%04x", (int) errorChar);
        else unexpected = "" + errorChar;

        String expected = ("'\uFFFF'".equals(target)) ? "end-of-input" : target;

        String summary = "Unexpected " + unexpected + " at input index:" +
                cursor() + "(line:" + line.lineNr + ",position:" + line.column +
                "), expected: " + expected;

        String detail = line.text; // TODO

        return new ParsingException(summary, detail);
    }

    public void parseJsValue() {
        ws();
        value();
        require(EOI);
    }

    private void ws() {
        while (((1L << cursorChar) & ((cursorChar - 64) >> 31) & 0x100002600L) != 0L) {
            cursorChar = nextChar();
        }
    }

    private boolean advance() {
        cursorChar = nextChar();
        return true;
    }

    public void value() {
        int mark = cursor();

        switch (cursorChar) {
            case 'f':
                if (!_false()) throw fail("JSON Value", mark);
                callback.onBoolean(false);
                break;
            case 'n':
                if (!_null()) throw fail("JSON Value", mark);
                callback.onNull();
                break;
            case 't':
                if (!_true()) throw fail("JSON Value", mark);
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
                break;
            case '\"':
                string();
                callback.onString(stringValue);
                break;
            default:
                throw fail("JSON Value");
        }

    }

    private boolean _false() {
        advance();
        return ch('a') && ch('l') && ch('s') && ws('e');
    }

    private boolean _null() {
        advance();
        return ch('u') && ch('l') && ws('l');
    }

    private boolean _true() {
        advance();
        return ch('r') && ch('u') && ws('e');
    }

    private boolean ch(char c) {
        if (cursorChar == c) {
            advance();
            return true;
        } else return false;
    }

    private void require(char c) {
        if (!ch(c))
            throw fail("'" + c + "'");
    }

    private boolean ws(char c) {
        if (ch(c)) {
            ws();
            return true;
        } else return false;
    }


    private int hexValue(char c) {
        if ('0' <= c && c <= '9') return c - '0';
        else if ('a' <= c && c <= 'f') return c - 87;
        else if ('A' <= c && c <= 'F') return c - 55;
        else throw fail("hex digit");
    }

    private void object() {
        ws();
        if (cursorChar != '}') {
            members();
        }
        require('}');
        ws();
    }

    private void members() {
        do {
            string();
            require(':');
            ws();

            callback.onStartField(stringValue);

            value();

            callback.onEndField();
            ws();

        } while (ws(','));
    }

    final void string(){
        if(cursorChar != '\"')
            throw fail("expect '\"'");

        final int begin = this.cursor;
        int end = -1;
        boolean existsEscape = false;
        final int length = this.length;

        int cursor = this.cursor + 1;
        for(; cursor < length; cursor++) {
            char cursorChar = charAt(cursor);
            if(cursorChar == '\"') {
                end = cursor;
                break;
            }
            else if(cursorChar == '\\') {
                if(!existsEscape){
                    resetBuffer();
                    for(int i = begin + 1; i < cursor; i++)
                        appendBuffer(charAt(i));
                }
                existsEscape = true;

                cursor = cursor + 1;
                if(cursor >= length) throw fail("escape");

                char esc0 = charAt(cursor);
                switch(esc0){
                    case '"':
                    case '/':
                    case '\\':
                    case '\'':
                        appendBuffer(esc0);  break;
                    case 'b':
                        appendBuffer('\b');  break;
                    case 'f':
                        appendBuffer('\f');  break;
                    case 'n':
                        appendBuffer( '\n');  break;
                    case 'r':
                        appendBuffer( '\r');  break;
                    case 't':
                        appendBuffer( '\t');  break;
                    case 'u':
                        if(cursor + 4 >= this.length) throw fail("escape");
                        int a = hexValue(charAt(cursor+1));
                        int b = hexValue(charAt(cursor+2));
                        int c = hexValue(charAt(cursor+3));
                        int d = hexValue(charAt(cursor+4));
                        cursor += 4;
                        int value = (a << 12)  | (b << 8) | (c << 4) | d;
                        appendBuffer ((char)value); break;
                    default:
                        throw fail("escape");

                }

            }
            else {
                if(existsEscape)
                    appendBuffer(cursorChar);
            }
        }
        if(end > begin) {
            if(!existsEscape) {
                this.stringValue = chars.substring(begin +1, end); // new String(chars, begin + 1, end);
            }
            else {
                this.stringValue = getBufferString();
                resetBuffer();
            }
            this.cursor = cursor + 1;
            this.cursorChar = charAt(this.cursor);
        }
        else throw fail("expect end '\"'");
    }

    private void array() {
        ws();
        int index = 0;
        if (cursorChar != ']') {
            do {
                callback.onStartField(index);
                value();
                callback.onEndField();
                ws();
                index++;
            } while (ws(','));
        }
        require(']');
        ws();
    }

    private void number() {
        int start = cursor();

        ch('-');
        _int();
        frac();
        exp();

        // TODO double = -1 * i * e ^ exp
        callback.onNumber(Double.parseDouble(new String(sliceCharArray(start, cursor()))));
        ws();
    }

    private void _int() {
        if (!ch('0')) oneOrMoreDigits();
    }

    private void frac() {
        if (ch('.')) oneOrMoreDigits();
    }

    private void exp() {
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

    private void zeroOrMoreDigits() {
        while (digit()) {
        }
    }

    private boolean digit() {
        return cursorChar >= '0' && cursorChar <= '9' && advance();
    }
}