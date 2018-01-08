package com.dapeng.soa

import java.util
/**
  * Created by lihuimin on 2018/1/4.
  */
class CalculateServiceImpl extends com.github.dapeng.soa.service.CalculateService{
  /**
    *
    **/
  override def calcualteWordCount(filename: String, word: String): Integer = new CalculateAction(filename, word).execute

  /**
    *
    **/
  override def calcualteWordsCount(fileName: String): util.Map[String, Integer] = ???
}
