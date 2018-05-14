package com.github.dapeng.impl.filters.freq;

/**
 * 描述: com.github.dapeng.impl.filters.freq
 *
 * @author hz.lei
 * @date 2018年05月14日 上午10:52
 */
public class DictionaryItem {

    short length;
    int id;
    /**
     * DictionaryData[ 2 * utf8offset ] 处开始存储这个字符串。
     */
    short utf8offset;

    @Override
    public String toString() {
        return "DictionaryItem{" + "length=" + length + ", id=" + id +
                ", utf8offset=" + utf8offset + '}';
    }
}
