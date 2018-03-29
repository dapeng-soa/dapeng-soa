package com.github.dapeng.core;

/**
 * @author with struy.
 * Create by 2018/3/28 21:40
 * email :yq1724555319@gmail.com
 * 自定的配置信息
 */

public class CustomConfigInfo {
    public long timeout = 2000;

    public CustomConfigInfo(){}

    public CustomConfigInfo(long timeout) {
        this.timeout = timeout;
    }



    @Override
    public String toString() {
        return "CustomConfigInfo{" +
                "timeout=" + timeout +
                '}';
    }
}
