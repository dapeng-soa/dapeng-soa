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
 * thrift -> json
 */
public class JsonWriter implements JsonCallback {

    private StringBuilder builder = new StringBuilder(64);

    @Override
    public void onStartObject() {
        builder.append('{');
    }

    @Override
    public void onEndObject() {
        removeTailSplitor();
        builder.append('}');
    }

    @Override
    public void onStartArray() {
        builder.append('[');
    }

    @Override
    public void onEndArray() {
        removeTailSplitor();
        builder.append(']');
    }

    @Override
    public void onStartField(String name) {
        builder.append('\"').append(name).append('\"').append(':');
    }

    @Override
    public void onStartField(int index) {
    }

    @Override
    public void onEndField() {
        builder.append(',');
    }

    @Override
    public void onBoolean(boolean value) {
        builder.append(value ? "true" : "false");
    }

    @Override
    public void onNumber(double value) {
        builder.append(value);
    }

    @Override
    public void onNumber(long value) {
        builder.append(value);
    }

    @Override
    public void onNull() {
        builder.append("null");
    }

    @Override
    public void onString(String value) {
        builder.append('\"');
        escapeString(value, builder);
        builder.append('\"');
    }

    private void removeTailSplitor() {
        int position = builder.length() - 1;
        if (builder.charAt(position) == ',') {
            builder.setLength(position);
        }
    }

    /**
     * 对回车以及双引号做转义
     * <p>
     * """\t\n\r"\"""
     * <p>
     * escapeString("\n\"\\") == "\\n\\"\\\\"
     *
     * @param value
     * @return
     */
    private void escapeString(String value, StringBuilder sb) {
        if (value != null && value.length() > 0) {
            int length = value.length();

            int index = 0;
            do {
                char ch = value.charAt(index++);
                switch (ch) {
                    case '\n':
                        sb.append("\\n");
                        break;
                    case '\t':
                        sb.append("\\t");
                        break;
                    case '\r':
                        sb.append("\\r");
                        break;
                    case '"':
                        sb.append("\\\"");
                        break;
                    case '\\':
                        sb.append("\\\\");
                        break;
                    default:
                        sb.append(ch);
                }

            } while (index < length);
        }
    }

    @Override
    public String toString() {
        return builder.toString();
    }
}
