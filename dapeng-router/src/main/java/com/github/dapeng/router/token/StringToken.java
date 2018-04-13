package com.github.dapeng.router.token;

/**
 * 描述:
 *
 * @author hz.lei
 * @date 2018年04月13日 下午9:06
 */
public class StringToken extends SimpleToken {

    public final String content;

    public StringToken(String content) {
        super(STRING);
        this.content = content;
    }
}
