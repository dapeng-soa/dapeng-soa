package com.github.dapeng.impl.filters.freq;

/**
 * Desc: 从内存读取出已经存入的CountNode信息
 *
 * @author hz.lei
 * @date 2018年05月14日 上午10:56
 */
public class MarkNode {
    long position;
    double rate;
    boolean isRemove = false;
    int key;

    public MarkNode(long position) {
        this.position = position;
    }

    @Override
    public String toString() {
        return "MarkNode{ position=" + position + ", rate=" + rate + ", isRemove=" + isRemove + ", key=" + key + '}';
    }
}
