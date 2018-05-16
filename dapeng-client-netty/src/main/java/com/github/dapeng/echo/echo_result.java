package com.github.dapeng.echo;

public class echo_result {

    private String success;

    public String getSuccess() {
        return success;
    }

    public void setSuccess(String success) {
        this.success = success;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("{");
        stringBuilder.append("\"").append("success").append("\":\"").append(this.success).append("\",");
        stringBuilder.deleteCharAt(stringBuilder.lastIndexOf(","));
        stringBuilder.append("}");

        return stringBuilder.toString();
    }
}