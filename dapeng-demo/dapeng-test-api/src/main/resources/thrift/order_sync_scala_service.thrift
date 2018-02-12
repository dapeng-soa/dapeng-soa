namespace java com.isuwang.soa.order.service

include "order_domain.thrift"

service OrderService {

    void createOrder(order_domain.Order order)

    order_domain.Order getOrderById(1: i32 orderId)
}