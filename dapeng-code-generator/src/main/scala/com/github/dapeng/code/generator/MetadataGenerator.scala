package com.github.dapeng.code.generator

import java.io._
import java.util
import javax.xml.bind.JAXB

import com.github.dapeng.core.metadata.{Service, Struct, TEnum}
import com.github.dapeng.core.metadata.{Service, Struct, TEnum}

/**
  * 源信息生成器
 *
  * @author craneding
  * @date 15/5/10
  */
class MetadataGenerator extends CodeGenerator {

  override def generate(services: util.List[Service], outDir: String, generateAll:Boolean , structs: util.List[Struct], enums:util.List[TEnum]): Unit = {}

  override def generate(services: util.List[Service], outDir: String): Unit = {
    println()
    println("***************源信息生成器***************")

    println(s"输出路径:${outDir}")

    // service.xml
    for (index <- (0 until services.size())) {
      val service = services.get(index)

      println(s"服务名称:${service.name}(${service.name}.xml)")

      val t1 = System.currentTimeMillis();

      JAXB.marshal(service, new FileOutputStream(new File(new File(outDir), s"${service.name}.xml")))

      val t2 = System.currentTimeMillis();

      println(s"生成耗时:${t2 - t1}ms")
      println(s"生成状态:完成")
    }

    println("***************源信息生成器***************")
  }


  def generateXmlFile(service:Service, outDir:String):Unit = {

    JAXB.marshal(service, new FileOutputStream(new File(new File(outDir), s"${service.namespace}.${service.name}.xml")))
  }

}
