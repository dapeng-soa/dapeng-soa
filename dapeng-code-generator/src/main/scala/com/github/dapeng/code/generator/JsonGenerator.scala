package com.github.dapeng.code.generator

import java.io._
import java.util

import com.github.dapeng.core.metadata.DataType.KIND
import com.github.dapeng.core.metadata.TEnum.EnumItem
import com.github.dapeng.core.metadata._
import com.github.dapeng.core.metadata.DataType.KIND
import com.github.dapeng.core.metadata.TEnum.EnumItem
import com.github.dapeng.core.metadata._

import scala.xml.Elem

/**
 * JSON生成器
 *
 * @author craneding
 * @date 15/5/10
 */
class JsonGenerator extends CodeGenerator {

  override def generate(services: util.List[Service], outDir: String, generateAll:Boolean , structs: util.List[Struct], enums:util.List[TEnum]): Unit = {}

  override def generate(services: util.List[Service], outDir: String): Unit = {
    println()
    println("***************源信息生成器***************")

    println(s"输出路径:${outDir}")

    // service.xml
    for (index <- (0 until services.size())) {
      val service = services.get(index)

      println(s"服务名称:${service.name}(${service.name}.json)")

      val t1 = System.currentTimeMillis();

      val codeTemplate = new StringTemplate(toServiceTemplate(service))

      val writer = new PrintWriter(new File(new File(outDir), s"${service.name}.json"))
      writer.write(codeTemplate.toString())
      writer.close()

      val t2 = System.currentTimeMillis();

      println(s"生成耗时:${t2 - t1}ms")
      println(s"生成状态:完成")
    }

    println("***************源信息生成器***************")
  }

  def toServiceTemplate(service: Service): Elem = {
    <div>
      // method

      {
          toMethodArrayBuffer(service.methods).map { (method: Method) =>
          <div>
            <block>
              "serviceName": "{service.name}",
              "version": "{service.meta.version}",
              "methodName": "{method.name}",
              "params":<block>
              {
                toFieldArrayBuffer(method.getRequest.getFields).filter(_.name != "requestHeader").map { (field:Field) =>
                  <div>
                    "{field.name}": <block>
                        {toDataTypeTemplate(field.dataType)}{if(field.optional) <span>//optional</span>}
                    </block> {if(field != method.request.fields.get(method.request.fields.size() - 1)) <span>,</span>}
                  </div>
                }
              }
              </block>
            </block>
          </div>
        }
      }

      // Enum

      {
          toTEnumArrayBuffer(service.enumDefinitions).map { (tEnum:TEnum) =>
            <div>
              /**
              * {tEnum.doc}
              **/
              export enum {tEnum.name} <block>
              {
              var index = 0;
              toEnumItemArrayBuffer(tEnum.enumItems).map { (enumItem:EnumItem) =>
                <div>
                  /**
                  * {enumItem.getDoc}
                  **/
                  {enumItem.getLabel} = {enumItem.getValue} {if(index != tEnum.enumItems.size() - 1) <span>,</span>}

                  {index = index + 1}
                </div>
              }
              }
            </block>
            </div>
          }
      }

      // Struct
      {
          toStructArrayBuffer(service.structDefinitions).map { (struct:Struct) =>
            <div>
              /**
              * {struct.doc}
              **/
              export class {struct.name} <block>
              {
              toFieldArrayBuffer(struct.fields).map { (field:Field) =>
                <div>
                  /**
                  * {field.doc}
                  **/
                  {field.name}:{toDataTypeTemplate(field.dataType)}{if(field.optional) <span>//optional</span>}
                </div>
              }
              }
            </block>
            </div>
          }
      }
    </div>
  }

  def toDataTypeTemplate(dataType:DataType): Elem = {
    dataType.kind match {
      case KIND.VOID => <div>void</div>
      case KIND.BOOLEAN => <div>boolean</div>
      case KIND.BYTE => <div>byte</div>
      case KIND.SHORT => <div>short</div>
      case KIND.INTEGER => <div>integer</div>
      case KIND.LONG => <div>long</div>
      case KIND.DOUBLE => <div>double</div>
      case KIND.STRING => <div>string</div>
      case KIND.BINARY => <div>binary</div>
      case KIND.MAP =>
        return {<div>Map{lt}{toDataTypeTemplate(dataType.getKeyType())}, {toDataTypeTemplate(dataType.getValueType())}{gt}</div>}
      case KIND.LIST =>
        return {<div>Array{lt}{toDataTypeTemplate(dataType.getValueType())}{gt}</div>}
      case KIND.SET =>
        return {<div>Array{lt}{toDataTypeTemplate(dataType.getValueType())}{gt}</div>}
      case KIND.ENUM =>
        val ref = dataType.getQualifiedName().replaceAll("^.*[.](.*?)$", "$1");
        return {<div>{ref}</div>}
      case KIND.STRUCT =>
        val ref = dataType.getQualifiedName().replaceAll("^.*[.](.*?)$", "$1");
        return {<div>{ref}</div>}
      case _ => <div></div>
    }
  }
}
