package com.github.dapeng.counter.service;

import com.github.dapeng.basic.api.counter.domain.DataPoint;
import com.github.dapeng.basic.api.counter.service.CounterService;
import com.github.dapeng.core.SoaException;
import com.github.dapeng.counter.dao.InfluxdbDao;

import java.util.List;

/**
 * author with struy.
 * Create by 2018/2/7 00:43
 * email :yq1724555319@gmail.com
 */

public class CounterServiceImpl implements CounterService {

    private InfluxdbDao  influxdbDao = new InfluxdbDao();

    @Override
    public void submitPoint(DataPoint point) throws SoaException {
        influxdbDao.writePoint(point);
    }

    @Override
    public void submitPoints(List<DataPoint> points) throws SoaException {
        influxdbDao.writePoints(points);
    }

    @Override
    public void queryPoints(DataPoint condition, String beginTimeStamp, String endTimeStamp) {

    }
}
