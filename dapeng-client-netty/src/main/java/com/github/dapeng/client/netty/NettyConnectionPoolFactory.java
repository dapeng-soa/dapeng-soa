package com.github.dapeng.client.netty;

import com.github.dapeng.core.SoaConnectionPool;
import com.github.dapeng.core.SoaConnectionPoolFactory;

/**
 * Created by lihuimin on 2017/12/24.
 */
public class NettyConnectionPoolFactory implements SoaConnectionPoolFactory {

    private static SoaConnectionPool pool = new SoaConnectionPoolImpl();

    @Override
    public SoaConnectionPool getPool() {

        return pool;
    }

}
