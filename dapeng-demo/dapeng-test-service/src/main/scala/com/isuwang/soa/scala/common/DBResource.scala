package com.isuwang.soa.scala.common

import javax.annotation.Resource
import javax.sql.DataSource

import scala.annotation.meta.{beanSetter, field}
import scala.beans.BeanProperty

/**
  * Created by jack on 2017/11/17
  */
object DBResource {
  def getInstance: DBResource.type = this

  @BeanProperty
  @(Resource @field @beanSetter)(name = "dataSource")
  var dataSource: DataSource = _

}
