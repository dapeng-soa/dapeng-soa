package com.github.dapeng.impl.filters.freq;

/**
 * 描述: nodePage元数据
 *
 * @author hz.lei
 * @date 2018年05月14日 上午10:51
 */
public class NodePageMeta {
    public final int hash;
    public short nodes;

    public NodePageMeta(int hash, short nodes) {
        this.hash = hash;
        this.nodes = nodes;
    }

    public void increaseNode() {
        this.nodes++;
    }

    @Override
    public String toString() {
        return "hash:" + hash + ", nodes:" + nodes;
    }
}
