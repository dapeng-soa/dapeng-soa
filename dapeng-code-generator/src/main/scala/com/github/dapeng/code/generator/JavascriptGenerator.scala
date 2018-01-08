package com.github.dapeng.code.generator

import java.io.{File, PrintWriter}
import java.util

import com.github.dapeng.core.metadata.DataType.KIND
import com.github.dapeng.core.metadata.TEnum.EnumItem
import com.github.dapeng.core.metadata._
import com.github.dapeng.core.metadata.DataType.KIND
import com.github.dapeng.core.metadata.TEnum.EnumItem
import com.github.dapeng.core.metadata._

import scala.xml.Elem

/**
  * Javascript生成器
 *
  * @author craneding
  * @date 15/7/22
  */
class JavascriptGenerator extends CodeGenerator {

  override def generate(services: util.List[Service], outDir: String, generateAll:Boolean , structs: util.List[Struct], enums:util.List[TEnum]): Unit = {}

  override def generate(services: util.List[Service], outDir: String): Unit = {
    println()
    println("*************JavaScript生成器*************")

    println(s"输出路径:${outDir}")

    // service.ts
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

    println("*************JavaScript生成器*************")
  }

  private def toServiceTemplate(service: Service): Elem = {
    return {
      <div>
        /**
        *
        {service.doc}
        **/
        module
        {service.name}<block>
        export class Helper
        <block>

          static fmt(s:any):any
          <block>
            if (typeof s == 'object'
            {and}{and}
            s != null) return Helper.json2str(s);
            return /^(string|number)$/.test(typeof s) ? "'" + s + "'" : s;
          </block>

          static json2str(o:any):string
          <block>
            var arr = [];
            for (var i in o)
            <block>
              if (i == 'toString')
              <block></block>
              else
              <block>
                arr.push("'" + i + "':" + Helper.fmt(o[i]));
              </block>
            </block>
            return '
            <block>' + arr.join(',') + '</block>
            ';
          </block>

        </block>{toTEnumArrayBuffer(service.enumDefinitions).map { (tEnum: TEnum) =>
          <div>
            /**
            *
            {tEnum.doc}
            **/
            export enum
            {tEnum.name}<block>
            {var index = 0;
            toEnumItemArrayBuffer(tEnum.enumItems).map { (enumItem: EnumItem) =>
              <div>
                /**
                *
                {enumItem.getDoc}
                **/
                {enumItem.getLabel}
                =
                {enumItem.getValue}{if (index != tEnum.enumItems.size() - 1) <span>,</span>}{index = index + 1}
              </div>
            }}
          </block>
          </div>
        }}{toStructArrayBuffer(service.structDefinitions).map { (struct: Struct) =>
          <div>
            /**
            *
            {struct.doc}
            **/
            export class
            {struct.name}<block>
            {toFieldArrayBuffer(struct.fields).map { (field: Field) =>
              <div>
                /**
                *
                {field.doc}
                **/
                {field.name}
                :
                {toDataTypeTemplate(field.dataType)}
              </div>
            }}
          </block>
          </div>
        }}

        /*
        * 错误信息
        **/
        export class Error
        <block>
          responseCode: string
          responseMsg:string

          constructor(responseCode, responseMsg)
          <block>
            this.responseCode = responseCode
            this.responseMsg = responseMsg
          </block>
        </block>

        /*
        * 异步结果
        **/
        export class AsyncResult
        {lt}
        T
        {gt}<block>
          promise:Promise

          constructor(promise:Promise)
          <block>
            this.promise = promise
          </block>

          success(successCallback: (promiseValue: T) => void) : AsyncResult
          {lt}
          T
          {gt}<block>
            this.promise.then(function onFulfilled(value)
            <block>
              successCallback(value)
            </block>
            )

            return this;
          </block>

          error(errorCallback: (promiseValue: Error) => void) : AsyncResult
          {lt}
          T
          {gt}<block>
            this.promise.catch(function onRejected(err)
            <block>
              errorCallback(err)
            </block>
            )

            return this;
          </block>
        </block>

        /*
        * 请求
        **/
        class Request
        <block>
          serviceName:string = "
          {service.name}
          "
          version:string = "
          {service.meta.version}
          "
          methodName:string
          params:any

          toString():string
          <block>
            return Helper.json2str(this);
          </block>
        </block>

        export class AsyncClient
        <block>

          static url:string = "/soaservice"

          {toMethodArrayBuffer(service.methods).map { (method: Method) => {
          <div>
            /*
            *
            {method.doc}
            **/
            static
            {method.name}
            (
            {toFieldArrayBuffer(method.getRequest.getFields).filter(_.name != "requestHeader").map { (field: Field) => {
            <div>
              {field.name}
              :
              {toDataTypeTemplate(field.getDataType())}{if (field != method.getRequest.fields.get(method.getRequest.fields.size() - 1)) <span>,</span>}
            </div>
          }
          }}
            ):AsyncResult
            {lt}{toDataTypeTemplate(method.getResponse.getFields().get(0).getDataType)}{gt}<block>
            var _request = new Request();

            _request.methodName = "
            {method.name}
            "

            _request.params =
            <block>
              {toFieldArrayBuffer(method.getRequest.getFields).filter(_.name != "requestHeader").map { (field: Field) =>
              <div>
                {field.name}
                :
                {field.name}{if (field != method.getRequest.fields.get(method.getRequest.fields.size() - 1)) <span>,</span>}
              </div>
            }}
            </block>

            var promise = new Promise(function (resolve, reject)
            <block>
              var xmlhttp;
              if (XMLHttpRequest)
              <block>// code for IE7+, Firefox, Chrome, Opera, Safari
                xmlhttp = new XMLHttpRequest()
              </block>
              else
              <block>// code for IE6, IE5
                xmlhttp = new ActiveXObject("Microsoft.XMLHTTP")
              </block>

              xmlhttp.onreadystatechange = function ()
              <block>
                if (xmlhttp.readyState == 4)
                <block>
                  if (xmlhttp.status == 200)
                  <block>
                    var result = eval('(' + xmlhttp.responseText + ')');
                    if(result.responseCode == '0')
                    <block>
                      resolve(result.success)
                    </block>
                    else
                    <block>
                      reject(new
                      {service.name}
                      .Error(result.responseCode, result.responseMsg))
                    </block>
                  </block>
                  else
                  <block>
                    reject(new
                    {service.name}
                    .Error(xmlhttp.statusText, xmlhttp.responseText))
                  </block>
                </block>
              </block>
              xmlhttp.open("POST", AsyncClient.url + "?t=" + Math.random(), true)
              xmlhttp.setRequestHeader("Content-Type", "application/x-www-form-urlencoded")
              xmlhttp.send(_request.toString())
            </block>
            );

            return new AsyncResult
            {lt}{toDataTypeTemplate(method.getResponse.getFields().get(0).getDataType)}{gt}
            (promise);
          </block>
          </div>
        }
        }}

        </block>

      </block>
      </div>
    }
  }

  def toDataTypeTemplate(dataType: DataType): Elem = {
    dataType.getKind() match {
      case KIND.VOID =>
        return {
          <div>any</div>
        }
      case KIND.BOOLEAN =>
        return {
          <div>
            {dataType.getKind.name().toLowerCase}
          </div>
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
          <div>
            {dataType.getKind.name().toLowerCase}
          </div>
        }
      case KIND.BINARY =>
        return {
          <div>any</div>
        }
      case KIND.MAP =>
        return {
          <div>Map
            {lt}{toDataTypeTemplate(dataType.getKeyType())}
            ,
            {toDataTypeTemplate(dataType.getValueType())}{gt}
          </div>
        }
      case KIND.LIST =>
        return {
          <div>Array
            {lt}{toDataTypeTemplate(dataType.getValueType())}{gt}
          </div>
        }
      case KIND.SET =>
        return {
          <div>Array
            {lt}{toDataTypeTemplate(dataType.getValueType())}{gt}
          </div>
        }
      case KIND.ENUM =>
        val ref = dataType.getQualifiedName().replaceAll("^.*[.](.*?)$", "$1");
        return {
          <div>
            {ref}
          </div>
        }
      case KIND.STRUCT =>
        val ref = dataType.getQualifiedName().replaceAll("^.*[.](.*?)$", "$1");
        return {
          <div>
            {ref}
          </div>
        }
      case _ => <div></div>
    }
  }

}
