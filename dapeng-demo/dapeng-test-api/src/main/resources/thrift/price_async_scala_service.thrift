namespace java com.isuwang.soa.price.service

include "price_domain.thrift"

service PriceService {

    void insertPrice(1: price_domain.Price price)

    list<price_domain.Price> getPrices()

}