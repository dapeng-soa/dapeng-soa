package com.github.dapeng.code.generator
import java.io.{File, PrintWriter}
import java.util

import com.github.dapeng.code.parser.ThriftCodeParser
import com.github.dapeng.core.metadata.DataType.KIND
import com.github.dapeng.core.metadata.TEnum.EnumItem
import com.github.dapeng.core.metadata._
import com.twitter.scrooge.ast.Document

import collection.JavaConverters._
import scala.xml.Elem

class TypeScriptGenerator(language: String) extends CodeGenerator{

  val thriftCodeParser =  new ThriftCodeParser(language)
  /**
    * 基于resource生成ts 文件， 一个thrift文件对应一个ts 文件
    * @param resources
    */
  def generate(resources: Array[String], cacheServices: List[Service], cacheStructs: List[Struct], cacheEnums: List[TEnum],
               outDir: String) = {

    //加载所有thrift 对象

    resources.foreach(resource => {
      val document: Document = thriftCodeParser.generateDoc(resource)

      val enums = if (document.enums != null && document.enums.nonEmpty) {
        val oriEnums = document.enums
        val tmpEnums = cacheEnums.filter(i => oriEnums.map(_.sid.name).contains(i.name))
        println(s"get enums: ${tmpEnums.map(_.name)}")
        tmpEnums
      } else List.empty

      val structs = if (document.structs != null && document.structs.nonEmpty) {
        val oriStruct = document.structs
        val tempStructs = cacheStructs.filter(i => oriStruct.map(_.originalName).contains(i.name))
        println(s" get structs: ${tempStructs.map(_.getName)}")
        tempStructs
      } else List.empty

      val services = if (document.services != null && document.services.nonEmpty) {
        val oriServices = document.services
        val tempServices = cacheServices.filter(i => oriServices.map(_.sid.name).contains(i.name))
        println(s" get services: ${tempServices.map(_.name)}")

        tempServices
      } else List.empty

      generateFile(services, structs, enums,resource, outDir)

      println(" ")
    })

  }

  override def generate(services: util.List[Service], outDir: String): Unit = {
    println("not supported generate params. please use generate(services: util.List[Service], outDir: String, generateAll: Boolean, structs: util.List[Struct], enums: util.List[TEnum]) instead....")
  }

  private def generateFile(services: List[Service], structs: List[Struct], enums: List[TEnum],resource: String, outDir: String): Unit = {

    println()
    println("*************TypeScriptScript生成器*************")

    val servicesTemplate = new StringTemplate(toServicesTemplate(services))
    val structsTemplate = new StringTemplate(toStructsTemplate(structs))
    val enumsTemplate =new StringTemplate(toEnumsTemplate(enums))
    val importTemplate = new StringTemplate(toImportTemplate(services, structs, enums))

    val oriFileName = new File(resource).getName
    val fileName = s"${oriFileName.substring(0,oriFileName.indexOf("."))}.ts"
    println(s" 生成文件: ${outDir + File.separator + fileName}")

    val writer = new PrintWriter(new File(new File(outDir), fileName))
    writer.write(importTemplate.toString)
    writer.write(enumsTemplate.toString)
    writer.write(structsTemplate.toString)
    writer.write(servicesTemplate.toString)
    writer.close()

    println("*************TypeScript 生成器*************")

  }

  /**
    * TODO: 需要根据resource判断 哪些数据类型需要import， 且import的类型属于哪个文件
    * 如： import {TaskRequest} from "./hello_domain"
    * @param services
    * @param structs
    * @param enums
    * @return
    */
  private def toImportTemplate(services: List[Service], structs: List[Struct], enums: List[TEnum]) = {
    <div>
      {if (services.nonEmpty || structs.nonEmpty || enums.nonEmpty)
          <div>import <block>metadata, RecordMeta, EnumMeta</block> from  "./meta"</div>
      }
    </div>

  }

  private def toStructsTemplate(structs: List[Struct]) = {
    {
      <div>
        {structs.map(struct => toStructTemplate(struct))}
      </div>
    }
  }

  private def toEnumsTemplate(enums: List[TEnum]) = {
    {
      <div>
        {enums.map(enum => toEnumTemplate(enum))}
      </div>
    }
  }

  private def toServicesTemplate(services: List[Service]) = {
    {
      <div>
        {services.map(service => toServiceTemplate(service))}
      </div>
    }
  }

  private def toStructTemplate(struct: Struct): Elem = {
    return {
      <div>
        /**
        * {struct.doc}
        */
        @metadata(<block>"name": "{struct.name}"{if (struct.annotations != null) <span>, {struct.annotations.asScala.map(i =>  {s""" "${i.key}" : "${i.value}"  """}).mkString(",")}</span>}</block>)
        export class {struct.name} <block>
            {struct.fields.asScala.map(field => {
          <div>
            @metadata(<block>"name": "{field.name}", "type": "{field.dataType.kind.name()}", "optional": "{field.optional}" , "visible": "{field.privacy}"{if (field.annotations != null) <span>, {field.annotations.asScala.map(i =>  {s""" "${i.key}" : "${i.value}"  """}).mkString(",")}</span>}</block>)
            {field.name}: {toDataTypeTemplate(field.dataType)}
          </div>
        })}

        static metadata = RecordMeta.buildRecord({struct.name}, [{struct.fields.asScala.map(i => s""" "${i.name}" """).mkString(",").map(i => <span>{i}</span>)}])
        </block>
      </div>
    }
  }


  private def toEnumTemplate(enum: TEnum): Elem = {
    return {
      <div>
        @metadata(<block>{if (enum.annotations != null) <span>, {enum.annotations.asScala.map(i =>  {s""" "${i.key}" : "${i.value}"  """}).mkString(",")}</span>}</block>)
        export enum {enum.name}<block>
        {enum.enumItems.asScala.map(i => {
            <div>
              {i.label} = {i.value},
            </div>
          })}

        </block>

      </div>
    }
  }

  private def toServiceTemplate(service: Service): Elem = {
    return {

      <div>

        /**
        *
        {service.doc}
        **/
        @metadata(<block>name: "{service.name}"</block>)
        class {service.name} <block>
            url: string;
            constructor (public url) <block>
                this.url = url
          </block>

              {service.methods.asScala.map(i => {
          <div>
            /**
            * {i.doc}
            */
            @metadata(<block>"name": "{i.name}", "response": "{i.response.fields.asScala.map{f => toDataTypeTemplate(f.dataType)}}" {if (i.annotations != null) <span>, {i.annotations.asScala.map(i => s""" "${i.key}" : "${i.value}"  """).mkString(",")}</span>}</block>)
            {i.name}(url: string,
              {i.request.fields.asScala.map{req =>
                  <div>
                    @metadata(<block>{req.annotations.asScala.map(i => s""" "${i.key}" : "${i.value}"  """).mkString(",").map(i => <span>{i}</span>)}</block>)
                    {req.name}: {toDataTypeTemplate(req.dataType)}
                  </div>}}): Promise{lt}{i.response.fields.asScala.map(resp => {toDataTypeTemplate(resp.dataType)})}{gt} <block>
                return null;
            </block>
          </div>
        })}

        </block>

      </div>
    }
  }

  def toDataTypeTemplate(dataType: DataType): Elem = {
    dataType.getKind() match {
      case KIND.VOID =>
        return {
          <div>VOID</div>
        }
      case KIND.BOOLEAN =>
        return {
          <div>{dataType.getKind.name().toLowerCase}</div>
        }
      case KIND.BYTE =>
        return {
          <div>number</div>
        }
      case KIND.SHORT =>
        return {
          <div>number</div>
        }
      case KIND.INTEGER =>
        return {
          <div>number</div>
        }
      case KIND.LONG =>
        return {
          <div>number</div>
        }
      case KIND.DOUBLE =>
        return {
          <div>number</div>
        }
      case KIND.STRING =>
        return {
          <div>{dataType.getKind.name().toLowerCase}</div>
        }
      case KIND.BINARY =>
        return {
          <div>any</div>
        }
      case KIND.MAP =>
        return {
          <div>Map{lt}{toDataTypeTemplate(dataType.getKeyType())}, {toDataTypeTemplate(dataType.getValueType())}{gt}</div>
        }
      case KIND.LIST =>
        return {
          <div>Array{lt}{toDataTypeTemplate(dataType.getValueType())}{gt}</div>
        }
      case KIND.SET =>
        return {
          <div>Array{lt}{toDataTypeTemplate(dataType.getValueType())}{gt}</div>
        }
      case KIND.ENUM =>
        val ref = dataType.getQualifiedName().replaceAll("^.*[.](.*?)$", "$1");
        return {
          <div>{ref}</div>
        }
      case KIND.STRUCT =>
        val ref = dataType.getQualifiedName().replaceAll("^.*[.](.*?)$", "$1");
        return {
          <div>{ref}</div>
        }
      case _ => <div></div>
    }
  }

  override def generate(services: util.List[Service], outDir: String, generateAll: Boolean, structs: util.List[Struct], enums: util.List[TEnum]): Unit = {
        for (index <- (0 until services.size())) {
          val service = services.get(index);

          println(s"服务名称:${service.name}(${service.name}.ts)")

          val t1 = System.currentTimeMillis();

          val codeTemplate = new StringTemplate(toServiceTemplate(service))

          val writer = new PrintWriter(new File(new File(outDir), s"${service.name}.ts"))
          writer.write(codeTemplate.toString())
          writer.close()

          println(s"生成耗时:${System.currentTimeMillis() - t1}ms")
          println(s"生成状态:完成")
        }

        if (enums != null) {
          enums.asScala.foreach(enum => {
            println(s" 枚举名称: ${enum.name} => ${enum.name}.ts")

            val t1 = System.currentTimeMillis();

            val template = new StringTemplate(toEnumTemplate(enum))
            val writer = new PrintWriter(new File(new File(outDir), s"${enum.name}.ts"))
            writer.write(template.toString())
            writer.close()

            println(s"生成 ${enum.name}.ts 耗时:${System.currentTimeMillis() - t1}ms")
            println(s"生成状态:完成")
          })


        }

        if (structs != null) {
          structs.asScala.foreach(struct => {
            println(s" 枚举名称: ${struct.name} => ${struct.name}.ts")

            val t1 = System.currentTimeMillis();

            val template = new StringTemplate(toStructTemplate(struct))
            val writer = new PrintWriter(new File(new File(outDir), s"${struct.name}.ts"))
            writer.write(template.toString())
            writer.close()

            println(s"生成 ${struct.name}.ts 耗时:${System.currentTimeMillis() - t1}ms")
            println(s"生成状态:完成")

          })
        }
  }
}
