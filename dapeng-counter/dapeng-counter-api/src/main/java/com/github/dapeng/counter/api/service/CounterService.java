package com.github.dapeng.counter.api.service;

import com.github.dapeng.core.SoaException;
import com.github.dapeng.counter.api.domain.DataPoint;
import java.util.List;

/**
 * author with struy.
 * Create by 2018/2/7 00:38
 * email :yq1724555319@gmail.com
 */

public interface CounterService {

    void submitPoint(DataPoint point) throws SoaException;

    void submitPoints(List<DataPoint> points) throws SoaException;

    void queryPoints(DataPoint condition,String beginTimeStamp, String endTimeStamp);

}
