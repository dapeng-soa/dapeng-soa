package com.isuwang.soa.service;

import com.github.dapeng.core.SoaException;
import com.isuwang.soa.price.domain.Price;
import com.isuwang.soa.price.service.PriceServiceAsync;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class PriceServiceAsyncImpl implements PriceServiceAsync{

    private static final List<Price> prices = new ArrayList<>();

    @Override
    public Future<Void> insertPrice(Price price) throws SoaException {
        System.out.println("=================insertPrice==================");
        return CompletableFuture.supplyAsync(() -> {
            prices.add(price);
            return null;
        });
    }

    @Override
    public Future<List<Price>> getPrices() throws SoaException {
        System.out.println("=================getPrices===============================");
        return CompletableFuture.supplyAsync(() -> prices);
    }
}
