/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.dapeng.counter.service;

import com.github.dapeng.basic.api.counter.domain.DataPoint;
import com.github.dapeng.basic.api.counter.service.CounterService;
import com.github.dapeng.core.SoaException;
import com.github.dapeng.counter.dao.InfluxdbDao;

import java.util.ArrayList;
import java.util.List;

/**
 * @author with struy.
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
    public List<DataPoint> queryPoints(DataPoint condition, String beginTimeStamp, String endTimeStamp) {
        return new ArrayList<>();
    }
}
