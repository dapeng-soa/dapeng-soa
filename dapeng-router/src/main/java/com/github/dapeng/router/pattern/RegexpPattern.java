package com.github.dapeng.router.pattern;

/**
 * 描述:
 *
 * @author hz.lei
 * @date 2018年04月13日 下午9:40
 */
public class RegexpPattern implements Pattern {

    public final String regex;

    public RegexpPattern(String regex) {

        this.regex = regex;
    }
}
