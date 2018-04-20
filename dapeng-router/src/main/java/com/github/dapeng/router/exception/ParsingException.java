package com.github.dapeng.router.exception;

/**
 * 描述: 路由词法解析 统一抛出的异常
 *
 * @author hz.lei
 * @date 2018年04月20日 下午1:07
 */
public class ParsingException extends RuntimeException {
    private String code;
    private String msg;

    public ParsingException(String code, String msg) {
        super(code + ":" + msg);
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
