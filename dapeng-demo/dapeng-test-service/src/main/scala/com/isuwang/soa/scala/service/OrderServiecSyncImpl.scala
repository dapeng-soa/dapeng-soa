package com.isuwang.soa.scala.service

import com.isuwang.soa.order.scala.domain.Order
import com.isuwang.soa.order.scala.service.OrderService
import com.isuwang.soa.scala.common.DBResource.dataSource
import wangzx.scala_commons.sql._

import scala.collection.mutable

class OrderServiecSyncImpl extends OrderService{

  /**
    *
    **/
  override def createOrder(order: Order): Unit = {
    println("=============== scala createOrder. =========")

    val orderSql = sql"INSERT INTO orders VALUES(${order.id},${order.order_no}, ${order.status}, ${order.amount})"
    dataSource.executeUpdate(orderSql)

  }

  /**
    *
    **/
  override def getOrderById(orderId: Int): Order = {
    println("=============== scala getOrderById. =========")
    val sql = sql" select * from orders where id = ${orderId}"

    val result = dataSource.row[com.isuwang.soa.scala.sql.demo.entity.Order](sql)
    println("=================================")
    println(s"Found orderResult:   ${result}")
    println("=================================")

    result match {
      case Some(r) =>
        BeanBuilder.build[Order](r)(
          "id" -> r.id,
          "order_no" -> r.orderNo,
          "status" -> r.status,
          "amount" -> r.amount
        )
      case None => null
    }

  }
}
