package com.github.dapeng.router.token;

/**
 * 描述: ID token
 *
 * @author hz.lei
 * @date 2018年04月13日 下午9:03
 */
public class IdToken extends SimpleToken {
    public final String name;

    public IdToken(String name) {
        super(ID);
        this.name = name;
    }

}
