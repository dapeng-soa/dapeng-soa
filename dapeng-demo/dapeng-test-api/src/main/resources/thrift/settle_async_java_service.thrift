namespace java com.isuwang.soa.settle.service

include "settle_domain.thrift"

service SettleService {

    void createSettle(settle_domain.Settle settle)

    settle_domain.Settle getSettleById(1: i32 settleId)
}