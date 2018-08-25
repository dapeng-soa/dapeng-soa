package com.github.dapeng.impl.filters.freq;

/**
 * 描述: DictionaryItem 字符串到id的映射区
 *
 * @author hz.lei
 * @date 2018年05月14日 上午10:52
 */
public class DictionaryItem {

    public final short length;
    public final int id;
    /**
     * DictionaryData[ 2 * utf8offset ] 处开始存储这个字符串。
     */
    public final short utf8offset;

    public DictionaryItem(short length, int id, short utf8offset) {
        this.length = length;
        this.id = id;
        this.utf8offset = utf8offset;
    }

    @Override
    public String toString() {
        return "DictionaryItem{" + "length=" + length + ", id=" + id +
                ", utf8offset=" + utf8offset + '}';
    }
}
