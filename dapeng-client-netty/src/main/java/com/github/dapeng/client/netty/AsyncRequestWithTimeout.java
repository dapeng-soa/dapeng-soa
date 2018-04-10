package com.github.dapeng.client.netty;

import java.util.concurrent.CompletableFuture;

/**
 *
 * @author tangliu
 * @date 2016/6/3
 */
public class AsyncRequestWithTimeout {

    public AsyncRequestWithTimeout(int seqid, long timeout, CompletableFuture future) {
        this.seqid = seqid;
        this.timeout = System.currentTimeMillis() + timeout;
        this.future = future;
    }

    public final long timeout;
    public final int seqid;
    public CompletableFuture<?> future;
}
