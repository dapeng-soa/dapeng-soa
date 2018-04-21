package com.github.dapeng.router.token;

/**
 * 描述: 各类符号等 词法解析单元 单个 char
 *
 * @author hz.lei
 * @date 2018年04月13日 下午9:01
 */
public class SimpleToken implements Token {

    public final int type;

    public SimpleToken(int type) {
        this.type = type;
    }

    @Override
    public int type() {
        return this.type;
    }

    @Override
    public String toString() {
        return "SimpleToken[type:" + type + "]";
    }
}
