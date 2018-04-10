package com.github.dapeng.json;

import com.github.dapeng.org.apache.thrift.TException;

public class JsonWriter implements JsonCallback {

    private StringBuilder builder = new StringBuilder(64);

    @Override
    public void onStartObject() {
        builder.append('{');
    }

    @Override
    public void onEndObject() {
        if (builder.charAt(builder.length() - 1) == ',') {
            builder.setLength(builder.length() - 1);
        }
        builder.append('}');
    }

    @Override
    public void onStartArray() {
        builder.append('[');
    }

    @Override
    public void onEndArray() {
        if (builder.charAt(builder.length() - 1) == ',') {
            builder.setLength(builder.length() - 1);
        }
        builder.append(']');
    }

    @Override
    public void onStartField(String name) {
        builder.append('\"').append(name).append('\"').append(':');
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
        builder.append('\"').append(escapeString(value)).append('\"');
    }

    /**
     * 对回车以及双引号做转义
     *
     * @param value
     * @return
     */
    private String escapeString(String value) {
        if (value != null) {
            if (value.contains("\n")) {
                value = value.replaceAll("[\n\r]", "\\\\n");
            }
            if (value.contains("\"")) {
                value = value.replaceAll("\"", "\\\\\"");
            }
        }
        return value;
    }

    @Override
    public String toString() {
        return builder.toString();
    }
}
