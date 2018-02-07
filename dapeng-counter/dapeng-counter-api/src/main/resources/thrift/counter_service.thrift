namespace java com.github.dapeng.basic.api.counter.service

include "counter_domain.thrift"

service CounterService {
    void submitPoint(1:counter_domain.DataPoint dataPoint)

    void submitPoints(1:list<counter_domain.DataPoint> dataPoints)

    list<counter_domain.DataPoint>    queryPoints  (1:counter_domain.DataPoint condition,2:string beginTimeStamp,3:string endTimeStamp)
}