package com.dapeng.soa

import org.slf4j.LoggerFactory

/**
  * Created by admin on 2017/8/16.
  */
class CalculateAction(fileName :String, word :String){

  val logger = LoggerFactory.getLogger(getClass)

  def execute: Int = {
    inputCheck

    action

  }





  def inputCheck: Unit = {}


  def action: Int = {

    println("");
    val result = 10;
    result
  }
  def postCheck: Unit = {}
}
