package com.github.dapeng.transaction.utils;

/**
 * Created by tangliu on 2016/4/12.
 */
public enum ErrorCode {


    NOTEXIST("transaction-err-002", "数据不存在"),

    INPUTERROR("transaction-err-001", "参数错误");

    private String code;
    private String msg;

    private ErrorCode(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public String getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
