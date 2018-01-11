package com.github.dapeng.core.filter;

import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.SoaException;

import java.util.List;

/**
 * Created by lihuimin on 2017/12/11.
 */
public class SharedChain implements FilterChain {

    public final Filter head;
    public final List<Filter> shared; // log->a->b->c
    public final Filter tail;
    public final int index;  // 0 -> n+2


    public int size() {
        return shared.size() + 2;
    }

    public SharedChain(Filter head, List<Filter> shared, Filter tail, int index) {
        if (index >= 2 + shared.size())
            throw new IllegalArgumentException();
        assert (head != null);
        assert (tail != null);

        this.head = head;
        this.shared = shared;
        this.tail = tail;
        this.index = index;

    }

    @Override
    public void onEntry(FilterContext ctx) throws SoaException {
        SharedChain next = null;
        if (index < 1 + shared.size())
            next = new SharedChain(head, shared, tail, index + 1);
        else next = null;

        if (index == 0) {
            head.onEntry(ctx, next);
        } else if (index > 0 && index < shared.size() + 1) {
            shared.get(index - 1).onEntry(ctx, next);
        } else if (index == shared.size() + 1) {
            tail.onEntry(ctx, next);
        }
    }

    @Override
    public void onExit(FilterContext ctx) throws SoaException {
        SharedChain prev = null;
        if (index >= 1)
            prev = new SharedChain(head, shared, tail, index - 1);
        else prev = null;

        if (index == 0) {
            head.onExit(ctx, null);
        } else if (index > 0 && index < shared.size() + 1) {
            shared.get(index - 1).onExit(ctx, prev);
        } else if (index == shared.size() + 1) {
            tail.onExit(ctx, prev);
        }
    }
}
