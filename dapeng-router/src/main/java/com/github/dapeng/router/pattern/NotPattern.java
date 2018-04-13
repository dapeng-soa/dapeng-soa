package com.github.dapeng.router.pattern;

/**
 * 描述:
 *
 * @author hz.lei
 * @date 2018年04月13日 下午9:39
 */
public class NotPattern implements Pattern {

    public final Pattern pattern;

    public NotPattern(Pattern pattern) {
        this.pattern = pattern;
    }

}
