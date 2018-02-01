package com.isuwang.soa.scala.service

import com.isuwang.soa.settle.scala.domain.Settle
import com.isuwang.soa.settle.scala.service.SettleServiceAsync

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


class SettleServiceAsyncImpl extends SettleServiceAsync{

  val settles = mutable.HashMap[Integer, Settle]()

  /**
    *
    **/
  override def createSettle(settle: Settle, timeout: Long) = {
    println("=================createSettle===================")
    Future {
      settles.put(settle.id,settle)
    }
  }

  /**
    *
    **/
  override def getSettleById(settleId: Int, timeout: Long) = {
    println(" =================getSettleById=================")
    Future {
      settles.get(settleId) match {
        case Some(s) => s
        case None => throw new Exception(s"Faild to find settle. settleId: ${settleId}")
      }
    }
  }
}
