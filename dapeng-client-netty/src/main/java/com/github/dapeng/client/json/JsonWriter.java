package com.github.dapeng.client.json;

public class JsonWriter implements JsonCallback {

    private StringBuilder builder = new StringBuilder(64);

    @Override
    public void onStartObject() {
        builder.append('{');
    }

    @Override
    public void onEndObject() {
        if (builder.charAt(builder.length()-1) == ',') {
            builder.setLength(builder.length()-1);
        }
        builder.append('}');
    }

    @Override
    public void onStartArray() {
        builder.append('[');
    }

    @Override
    public void onEndArray() {
        if (builder.charAt(builder.length()-1) == ',') {
            builder.setLength(builder.length()-1);
        }
        builder.append(']');
    }

    @Override
    public void onStartField(String name) {
        // TODO emit ','
        builder.append('\"').append(name).append('\"').append(':');
    }

    @Override
    public void onEndField() {
        builder.append(',');
    }

    @Override
    public void onBoolean(boolean value) {
        builder.append( value ? "true" : "false");
    }

    @Override
    public void onNumber(double value) {
        builder.append(value);
    }

    @Override
    public void onNull() {
        builder.append("null");
    }

    @Override
    public void onString(String value) {
        // TODO escapse if needed
        builder.append('\"').append(value).append('\"');
    }

    @Override
    public String toString() {
        return builder.toString();
    }
}
