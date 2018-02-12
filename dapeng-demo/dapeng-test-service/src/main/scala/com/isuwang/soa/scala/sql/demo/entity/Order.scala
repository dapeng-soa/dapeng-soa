package com.isuwang.soa.scala.sql.demo.entity

import wangzx.scala_commons.sql.ResultSetMapper

case class Order(id : Int, orderNo : String, status : Int, amount : Double)

object Order {
  implicit val resultSetMapper: ResultSetMapper[Order] = ResultSetMapper.material[Order]
}


