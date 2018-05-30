package com.github.dapeng.router.pattern;

/**
 * 描述: 正则 条件表达式
 * etc:
 * <p>
 * method match r"setFoo.*"
 *
 * @author hz.lei
 * @date 2018年04月13日 下午9:40
 */
public class RegexPattern implements Pattern {

    public final java.util.regex.Pattern pattern;

    public RegexPattern(java.util.regex.Pattern pattern) {

        this.pattern = pattern;
    }

    @Override
    public String toString() {
        return "RegexPattern{" +
                "pattern=" + pattern +
                '}';
    }
}
