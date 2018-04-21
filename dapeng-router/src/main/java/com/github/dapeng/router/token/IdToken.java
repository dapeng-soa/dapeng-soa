package com.github.dapeng.router.token;

/**
 * 描述: id 词法解析单元，作为一个被匹配的类型的名称
 * etc.
 * <p>
 * method match
 * version match
 * userId match
 * method version userId 等诸如此类的都称为ID
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

    @Override
    public String toString() {
        return "IdToken[type:" + type + ", name:" + name + "]";
    }
}
