package com.github.dapeng.client.netty;

import java.util.concurrent.CompletableFuture;

/**
 * Created by tangliu on 2016/6/3.
 */
public class AsyncRequestWithTimeout {

    public AsyncRequestWithTimeout(int seqid, long timeout, CompletableFuture future) {

        this.seqid = seqid;
        this.timeout = System.currentTimeMillis() + timeout;
        this.future = future;
    }

    private long timeout;

    private int seqid;

    private CompletableFuture<?> future;

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public int getSeqid() {
        return seqid;
    }

    public void setSeqid(int seqid) {
        this.seqid = seqid;
    }

    public CompletableFuture getFuture() {
        return future;
    }

    public void setFuture(CompletableFuture future) {
        this.future = future;
    }
}
