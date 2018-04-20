package com.github.dapeng.router.exception;

/**
 * 描述: 路由词法解析 统一抛出的异常
 *
 * @author hz.lei
 * @date 2018年04月20日 下午1:07
 */
public class ParsingException extends RuntimeException {
    private final String summary;
    private final String detail;

    public ParsingException(String summary, String detail) {
        super(summary + ":" + detail);
        this.summary = summary;
        this.detail = detail;
    }
}
