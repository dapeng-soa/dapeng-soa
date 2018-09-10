package com.github.dapeng.code.generator

import java.io._
import java.util
import javax.xml.bind.{DataBindingException, JAXB}

import com.github.dapeng.core.{SoaCode, SoaException}
import com.github.dapeng.core.metadata.{Service, Struct, TEnum}

/**
  * 源信息生成器
  *
  * @author craneding
  * @date 15/5/10
  */
class MetadataGenerator extends CodeGenerator {

  override def generate(services: util.List[Service], outDir: String, generateAll: Boolean, structs: util.List[Struct], enums: util.List[TEnum]): Unit = {}

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


  def generateXmlFile(service: Service, outDir: String, tryCount: Int): Unit = {
    val reGenCount = tryCount +1
   JAXB.marshal(service, new FileOutputStream(new File(new File(outDir), s"${service.namespace}.${service.name}.xml")))
    val xmlTargetPath = s"$outDir${service.namespace}.${service.name}.xml"
    //println(s"检查xml => $xmlTargetPath")
    try {
      //val checkServiceObj: Service = JAXB.unmarshal(xmlTargetPath, classOf[Service])
      val checkServiceObj: Service = JAXB.unmarshal(new File(xmlTargetPath).toURI, classOf[Service])
      if (checkServiceObj == null) {
        if (reGenCount <= XML_REGEN_COUNT) {
          println(s" Re-Gen Xml次数:$reGenCount")
          generateXmlFile(service, outDir, reGenCount)
        }else{
          throw new SoaException(SoaCode.ServerUnKnown, s"${service.namespace}.${service.name}.xml 文件生成有误")
        }
      }else{
        println(s"检查xml格式=>${service.namespace}.${service.name}.xml 无误")
      }
    } catch {
      case ex: DataBindingException =>
        if (reGenCount <= XML_REGEN_COUNT) {
          println(ex.getMessage)
          println(s" Re-Gen Xml次数:$reGenCount")
          generateXmlFile(service, outDir, reGenCount)
        }else{
          throw new SoaException(SoaCode.ServerUnKnown, s"${service.namespace}.${service.name}.xml 文件生成有误: ${ex.getMessage}")
        }
      case ex: IOException =>  throw new SoaException(SoaCode.ServerUnKnown, s"${service.namespace}.${service.name}.xml 文件生成有误: ${ex.getMessage}")
    }
  }

}
