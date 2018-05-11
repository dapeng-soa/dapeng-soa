package com.github.dapeng.router.token;

/**
 * 描述: string 字符串, 词法解析单元
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

    @Override
    public String toString() {
        return "StringToken[type:" + type + ", content:" + content + "]";
    }
}
