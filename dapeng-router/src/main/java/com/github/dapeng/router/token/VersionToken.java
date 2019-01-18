package com.github.dapeng.router.token;

/**
 * 描述: 版本 词法解析单元
 *
 * @author huyj
 * @Created 2019-01-17 10:52
 */
public class VersionToken extends SimpleToken {
    public final String version;

    public VersionToken(String version) {
        super(VERSION);
        this.version = version;
    }

    @Override
    public String toString() {
        return "VersionToken{" +
                "version='" + version + '\'' +
                ", type=" + type +
                '}';
    }
}
