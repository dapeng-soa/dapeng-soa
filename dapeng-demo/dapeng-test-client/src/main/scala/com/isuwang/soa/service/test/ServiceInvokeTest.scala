package com.isuwang.soa.service.test

import java.util.Optional
import java.util.concurrent.{CompletableFuture, TimeUnit}

import com.isuwang.soa.order.{OrderServiceAsyncClient, OrderServiceClient}
import com.isuwang.soa.price.{PriceServiceAsyncClient, PriceServiceClient}
import com.isuwang.soa.price.domain.Price
import com.isuwang.soa.settle.{SettleServiceAsyncClient, SettleServiceClient}
import com.isuwang.soa.user.{UserServiceAsyncClient, UserServiceClient}

import _root_.scala.concurrent.ExecutionContext.Implicits.global
import _root_.scala.util.Success
import _root_.scala.util.Failure

object ServiceInvokeTest {

  /**
    *  priceServiceClient (java) -> PriceServiceServer (java Async)
    *  priceServiceClient (scala) -> PriceServiceServer (java Async)
    *  AsyncpriceServiceClient (java) -> PriceServiceServer (java Async)
    *  AsyncpriceServiceClient (scala) -> PriceServiceServer (java Async)
    *
    *  SettleServiceClient (java) -> SettleServiceServer (scala Async)
    *  SettleServiceClient (scala) -> SettleServiceServer (scala Async)
    *  AsyncSettleServiceClient (java) -> SettleServiceServer (scala Async)
    *  AsyncSettleServiceClient (scala) -> SettleServiceServer (scala Async)
    *
    *  UserServiceClient (java) -> UserServiceServer (java Sync)
    *  UserServiceClient (scala) -> UserServiceServer (java Sync)
    *  AsyncUserServiceClient (scala) -> UserServiceServer (java sync)
    *  AsyncUserServiceClient (java) -> UserServiceServer (java sync)
    *
    *  OrderServiceClient (java) -> OrderServiceServer (scala Sync)
    *  OrderServiceClient (scala) -> OrderServiceServer (scala Sync)
    *  AsyncOrderServiceClient (java) -> OrderServiceServer (scala sync)
    *  AsyncOrderServiceClient (scala) -> OrderServiceServer (scala sync)
    *
    * @param args
    */
  def main(args: Array[String]): Unit = {


    new PriceServiceClient().insertPrice(new Price().orderId(1).price(1000.0))
    new com.isuwang.soa.price.scala.PriceServiceClient().insertPrice(com.isuwang.soa.price.scala.domain.Price(2,2000.0))
    val javaPrices = new PriceServiceAsyncClient().getPrices().get(3000, TimeUnit.MILLISECONDS)
    println(s" javaPrices: ${javaPrices}")
//    assert(javaPrices.size() == 2, " javaPrices size not match")
    new com.isuwang.soa.price.scala.PriceServiceAsyncClient().getPrices().onComplete({
      case Success(scalaPrices) => println(s" scalaPrices: ${scalaPrices}")
      case Failure(e) => throw e
    })

    new SettleServiceClient().createSettle(new com.isuwang.soa.settle.domain.Settle()
      .id(1).orderId(1).cash_credit(1000).cash_debit(100).remark(Optional.of("settle remark")))
    new com.isuwang.soa.settle.scala.SettleServiceClient().createSettle(com.isuwang.soa.settle.scala.domain.Settle(2,2,2000,2000,Option.apply("scala settle remark")))
    val javaSettle = new SettleServiceAsyncClient().getSettleById(1).get(3000, TimeUnit.MILLISECONDS)
    println(s" javaSettle: ${javaSettle}")
    new com.isuwang.soa.settle.scala.SettleServiceAsyncClient().getSettleById(2).onComplete({
      case Success(scalaSettle) => println(s"scalaSettle: ${scalaSettle}")
      case Failure(e) => throw e
    })


    new UserServiceClient().createUser(new com.isuwang.soa.user.domain.User().id(1).name("userName1").password("123456").phoneNumber("1234567890"))

    new com.isuwang.soa.user.scala.UserServiceClient().createUser(com.isuwang.soa.user.scala.domain.User(2,"userName2","123","123456"))
    val javaUser = new UserServiceAsyncClient().getUserById(1).get(3000, TimeUnit.MILLISECONDS)
    println(s" javaUser: ${javaUser}")
    new com.isuwang.soa.user.scala.UserServiceAsyncClient().getUserById(2).onComplete({
      case Success(scalaUser) => println(s" scalaUser: ${scalaUser}")
      case Failure(e) => throw e
    })


    new OrderServiceClient().createOrder(new com.isuwang.soa.order.domain.Order().id(1).order_no("1").status(1).amount(1000))
    new com.isuwang.soa.order.scala.OrderServiceClient().createOrder(com.isuwang.soa.order.scala.domain.Order(2,"2",2,2000))

    val javaOrder = new OrderServiceAsyncClient().getOrderById(1).get(3000, TimeUnit.MILLISECONDS)
    println(s" javaOrder: ${javaOrder}")
    new com.isuwang.soa.order.scala.OrderServiceAsyncClient().getOrderById(2).onComplete({
      case Success(scalaOrder) => println(s" scalaOrder: ${scalaOrder}")
      case Failure(e) => throw e
    })



  }
}
