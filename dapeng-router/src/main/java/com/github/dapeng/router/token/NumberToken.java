package com.github.dapeng.router.token;

/**
 * 描述:
 *
 * @author hz.lei
 * @date 2018年04月13日 下午9:07
 */
public class NumberToken extends SimpleToken {

    public final int number;

    public NumberToken(int number) {
        super(NUMBER);
        this.number = number;
    }
}
