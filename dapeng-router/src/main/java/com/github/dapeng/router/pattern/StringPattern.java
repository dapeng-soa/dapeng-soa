package com.github.dapeng.router.pattern;

/**
 * 描述: 字符串条件表达式
 *
 * @author hz.lei
 * @date 2018年04月13日 下午9:39
 */
public class StringPattern implements Pattern {

    public final String content;

    public StringPattern(String content) {

        this.content = content;
    }

    @Override
    public String toString() {
        return "StringPattern{" +
                "content='" + content + '\'' +
                '}';
    }
}
