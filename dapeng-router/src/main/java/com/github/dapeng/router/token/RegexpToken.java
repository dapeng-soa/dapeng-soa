package com.github.dapeng.router.token;

/**
 * 描述: 正则  词法解析单元
 *
 * @author hz.lei
 * @date 2018年04月13日 下午9:07
 */
public class RegexpToken extends SimpleToken {

    public final String regexp;

    public RegexpToken(String regexp) {
        super(REGEXP);
        this.regexp = regexp;
    }
}
