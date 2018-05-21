package com.github.dapeng.impl.filters.freq;

/**
 * Desc: 从内存读取出已经存入的CountNode信息
 *
 * @author hz.lei
 * @date 2018年05月14日 上午10:56
 */
public class MarkNode {

    public final long position;
    public double rate;
    public boolean isRemove = false;
    public int key;

    public MarkNode(final long position) {
        this.position = position;
    }

    @Override
    public String toString() {
        return "MarkNode{ position=" + position + ", rate=" + rate + ", isRemove=" + isRemove + ", key=" + key + '}';
    }
}
