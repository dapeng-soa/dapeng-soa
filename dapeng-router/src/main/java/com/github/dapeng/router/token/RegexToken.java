package com.github.dapeng.router.token;


import java.util.regex.Pattern;

/**
 * 描述: 正则  词法解析单元
 *
 * @author hz.lei
 * @date 2018年04月13日 下午9:07
 */
public class RegexToken extends SimpleToken {

    public final Pattern pattern;

    public RegexToken(String regex) {
        super(REGEXP);
        pattern = Pattern.compile(regex);
    }

    @Override
    public String toString() {
        return "RegexToken[type:" + type + ", pattern:" + pattern + "]";
    }
}
