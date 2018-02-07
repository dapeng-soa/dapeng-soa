package com.github.dapeng.counter.dao;

import com.github.dapeng.basic.api.counter.domain.DataPoint;
import com.github.dapeng.counter.util.CounterServiceProperties;
import org.influxdb.dto.Point;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * author with struy.
 * Create by 2018/2/7 10:08
 * email :yq1724555319@gmail.com
 */

public class InfluxdbDao {
    private static final Logger LOGGER = LoggerFactory.getLogger(InfluxdbDao.class);

    private final String INFLUXDB_URL = CounterServiceProperties.SOA_COUNTER_INFLUXDB_URL;
    private final String INFLUXDB_USER = CounterServiceProperties.SOA_COUNTER_INFLUXDB_USER;
    private final String INFLUXDB_PWD = CounterServiceProperties.SOA_COUNTER_INFLUXDB_PWD;
    private final InfluxDB influxDB = getInfluxDBConnection();

    public InfluxDB getInfluxDBConnection() {
        LOGGER.info("Connection InfluxDB on :{}", INFLUXDB_URL);
        return InfluxDBFactory.connect(INFLUXDB_URL, INFLUXDB_USER, INFLUXDB_PWD);
    }

    public void writePoint(DataPoint dataPoint) {
        Point.Builder commit = Point.measurement(dataPoint.bizTag);
        dataPoint.values.forEach(commit::addField);
        dataPoint.tags.forEach(commit::tag);
        try {
            influxDB.write(dataPoint.database, "", commit.build());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            influxDB.close();
        }
    }

    public void writePoints(List<DataPoint> dataPoints) {
        LOGGER.info("counter writePoints {}", dataPoints);
        dataPoints.forEach(this::writePoint);
        /*if (dataPoints.size()!=0){
            BatchPoints batchPoints = BatchPoints
                    .database(dataPoints.get(0).getDatabase())
                    .retentionPolicy("default")
                    .consistency(InfluxDB.ConsistencyLevel.ALL)
                    .build();
            dataPoints.forEach((DataPoint dataPoint) -> {
                Point point = Point.measurement(dataPoint.bizTag)
                        .fields(dataPoint.values) // todo
                        .tag(dataPoint.tags)
                        .build();
                batchPoints.point(point);
            });
            InfluxDB influxDB =  getInfluxDBConnection();
            try {
                influxDB.write(batchPoints);
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                influxDB.close();
            }
        }*/
    }


}
