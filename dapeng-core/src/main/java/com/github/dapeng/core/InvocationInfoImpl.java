package com.github.dapeng.core;

/**
 * Created by lihuimin on 2017/12/22.
 */
public class InvocationInfoImpl implements InvocationContext.InvocationInfo {

    public final int seqid;

    public InvocationInfoImpl(int seqid){

        this.seqid = seqid;
    }
}
