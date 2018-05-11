package com.github.dapeng.router.pattern;

/**
 * 描述: 整数匹配条件表达式
 *
 * @author hz.lei
 * @date 2018年04月13日 下午9:41
 */
public class NumberPattern implements Pattern {

    public final int number;

    public NumberPattern(int number) {
        this.number = number;
    }

}
