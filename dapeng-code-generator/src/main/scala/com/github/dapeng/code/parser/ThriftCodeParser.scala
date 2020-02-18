///*
// * Licensed to the Apache Software Foundation (ASF) under one or more
// * contributor license agreements.  See the NOTICE file distributed with
// * this work for additional information regarding copyright ownership.
// * The ASF licenses this file to You under the Apache License, Version 2.0
// * (the "License"); you may not use this file except in compliance with
// * the License.  You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package com.github.dapeng.code.parser
//
//import java.io._
//import java.util
//
//import com.github.mustachejava.Mustache
//import com.google.common.base.Charsets
//import com.google.common.io.CharStreams
//import com.github.dapeng.core.metadata
//import com.github.dapeng.core.metadata.TEnum.EnumItem
//import com.github.dapeng.core.metadata.{Annotation, DataType, Method, TEnum}
//import com.twitter.scrooge.ast._
//import com.twitter.scrooge.frontend.{Importer, ResolvedDocument, ThriftParser, TypeResolver}
//import com.twitter.scrooge.java_generator._
//
//import scala.collection.JavaConversions._
//import scala.collection.concurrent.TrieMap
//import scala.collection.mutable
//import scala.io.Source
//import scala.util.control.Breaks._
//
//
///**
//  * Thrift Code 解析器
//  *
//  * @author craneding
//  * @date 15/7/22
//  */
//class ThriftCodeParser(var language: String) {
//
//  private val templateCache = new TrieMap[String, Mustache]
//  private val docCache = new mutable.HashMap[String, Document]()
//  private val enumCache = new util.ArrayList[TEnum]()
//  private val structCache = new util.ArrayList[metadata.Struct]()
//  private val serviceCache = new util.ArrayList[metadata.Service]()
//
//  private val mapStructCache = new util.HashMap[String, metadata.Struct]()
//  private val mapEnumCache = new util.HashMap[String, metadata.TEnum]()
//
//  /**
//    * like
//    * val namespace = com.github.dapeng.soa.service
//    * toScalaNamespace(namespace) => com.github.dapeng.soa.scala.service
//    * toScalaNamespace(namespace, 1) => com.github.dapeng.scala.soa.service
//    *
//    * @param namespace
//    * @param lastIndexCount
//    * @return
//    */
//  private def toScalaNamespace(namespace: String, lastIndexCount: Int = 0) = {
//    if (namespace != null && namespace.length > 0) {
//      var int = lastIndexCount
//      var beginStr = namespace
//      var endStr = ""
//      while ((int >= 0)) {
//        endStr = s"${beginStr.substring(beginStr.lastIndexOf("."))}${endStr}"
//        beginStr = beginStr.substring(0, beginStr.lastIndexOf("."))
//        int -= 1
//      }
//      s"${beginStr}.scala${endStr}"
//    } else namespace
//  }
//
//  /**
//    * 生成文档
//    *
//    * @param resource 源文件
//    * @return 文档
//    */
//  private def generateDoc(resource: String): Document = {
//
//    val homeDir = resource.substring(0, if (resource.lastIndexOf("/") == -1) resource.lastIndexOf("\\") else resource.lastIndexOf("/"))
//
//    val br = new BufferedReader(new InputStreamReader(new FileInputStream(resource), Charsets.UTF_8))
//    val txt = CharStreams.toString(br)
//
//    //如果是scala，需要重写namespace
//    val finalTxt = if (language.equals("scala")) {
//      // getNamespaceLine => "namespace java xxxxxx.xx.xx"
//      val namespaceLine = Source.fromFile(resource).getLines().find(_.trim.startsWith("namespace"))
//      namespaceLine match {
//        case Some(i) =>
//          val namespace = i.split(" ").reverse.head
//          val scalaNamespace = toScalaNamespace(namespace)
//          txt.replace(namespace,scalaNamespace)
//        case None =>
//          println("[Warning] you should specific your namespace statement at head of your thrift file. like: namespace java YourPackageName")
//          txt
//      }
//    } else txt
//
//    val importer = Importer(Seq(homeDir))
//    val parser = new ThriftParser(importer, true)
//    val doc = parser.parse(finalTxt, parser.document)
//
//    try {
//      TypeResolver()(doc).document
//    } finally {
//      println(s"parse ${resource} success")
//    }
//  }
//
//  /**
//    * 获取生成器
//    *
//    * @param doc0        文档结构
//    * @return 生成器
//    */
//  private def getGenerator(doc0: Document): ApacheJavaGenerator = {
//    new ApacheJavaGenerator(new ResolvedDocument(doc0, new TypeResolver()), "thrift", templateCache)
//    //new ApacheJavaGenerator(Map(), "thrift", templateCache, genHashcode = genHashcode)
//  }
//
//  private def toDocString(docstring: scala.Option[String]): String =
//    if (docstring == None)
//      null
//    else {
//      val result = docstring.get.toString();
//
//      val in = new BufferedReader(new StringReader(result));
//
//      val buffer = new StringBuilder();
//
//      breakable {
//        while (true) {
//          var line = in.readLine();
//
//          if (line == null)
//            break;
//
//          //          line = line.trim();
//
//          if (line.matches("^\\s*[*]{1,2}/$")) {
//            line = ""
//          } else if (line.matches("^\\s*[*].*$")) {
//            line = line.trim();
//            line = line.substring(1)
//          } else if (line.matches("^\\s*/[*]{2}.*$")) {
//            line = line.trim();
//            line = line.substring("/**".length)
//          }
//
//          if (line.endsWith("**/"))
//            line = line.substring(0, line.lastIndexOf("**/"))
//          if (line.endsWith("*/"))
//            line = line.substring(0, line.lastIndexOf("*/"))
//
//          if (line.length > 0) {
//            if (buffer.length > 0)
//              buffer.append("\n")
//            if (line.trim.startsWith("#")) {
//              buffer.append(line.trim);
//            } else {
//              buffer.append(line);
//            }
//
//          } else {
//            buffer.append("\n");
//          }
//        }
//      }
//
//      in.close()
//
//      //result = result.replace("/**", "").replace("**/", "").replace("*/", "").trim;
//      //val pattern = "[ ]*[*][\\s]?".r;
//      //result = (pattern replaceAllIn(result, ""));
//
//      return buffer.toString();
//    }
//
//
//  private def toDataType(fieldType: FieldType, defaultDoc: Document, docString: String): DataType = {
//    val dataType = new DataType()
//
//    fieldType match {
//      case _: BaseType =>
//        val clazz = fieldType.getClass
//
//        if (clazz == TI16.getClass) {
//          dataType.setKind(DataType.KIND.SHORT)
//        } else if (clazz == TI32.getClass) {
//          dataType.setKind(DataType.KIND.INTEGER)
//        } else if (clazz == TI64.getClass) {
//          dataType.setKind(DataType.KIND.LONG)
//
//          //2016-2-18 In order to generate Date type
//          if (docString != null && docString.toLowerCase.contains("@datatype(name=\"date\")"))
//            dataType.setKind(DataType.KIND.DATE)
//
//        } else if (clazz == TDouble.getClass) {
//          dataType.setKind(DataType.KIND.DOUBLE)
//
//          //2016-4-08 In order to generate BigDecimal type
//          if (docString != null && docString.toLowerCase.contains("@datatype(name=\"bigdecimal\")"))
//            dataType.setKind(DataType.KIND.BIGDECIMAL)
//
//        } else if (clazz == TByte.getClass) {
//          dataType.setKind(DataType.KIND.BYTE)
//        } else if (clazz == TBool.getClass) {
//          dataType.setKind(DataType.KIND.BOOLEAN)
//        } else if (clazz == TString.getClass) {
//          dataType.setKind(DataType.KIND.STRING)
//        } else {
//          dataType.setKind(DataType.KIND.BINARY)
//        }
//      case EnumType(enum, scopePrefix) =>
//        dataType.setKind(DataType.KIND.ENUM)
//
//        val doc1 = if (scopePrefix != None) docCache(scopePrefix.get.name) else defaultDoc
//        val enumController = new EnumController(enum, getGenerator(doc1), getNameSpace(doc1, language))
//
//        dataType.setQualifiedName(enumController.namespace + "." + enumController.name)
//
//      case StructType(struct, scopePrefix) =>
//        dataType.setKind(DataType.KIND.STRUCT)
//
//        val doc1 = if (scopePrefix != None) docCache(scopePrefix.get.name) else defaultDoc
//        dataType.setQualifiedName(doc1.namespace("java").get.fullName + "." + struct.originalName)
//      case _: ListType =>
//        dataType.setKind(DataType.KIND.LIST)
//
//        dataType.setValueType(toDataType(fieldType.asInstanceOf[ListType].eltType, defaultDoc, docString))
//      case _: SetType =>
//        dataType.setKind(DataType.KIND.SET)
//
//        dataType.setValueType(toDataType(fieldType.asInstanceOf[SetType].eltType, defaultDoc, docString))
//      case _: MapType =>
//        dataType.setKind(DataType.KIND.MAP)
//
//        dataType.setKeyType(toDataType(fieldType.asInstanceOf[MapType].keyType, defaultDoc, docString))
//        dataType.setValueType(toDataType(fieldType.asInstanceOf[MapType].valueType, defaultDoc, docString))
//      case _ =>
//        dataType.setKind(DataType.KIND.VOID)
//    }
//
//    dataType
//  }
//
//  private def getNameSpace(doc: Document, language: String): Option[Identifier] = {
//    doc.namespace(language) match {
//      case x@Some(id) => x
//      case None => // return a default namespace of java
//        doc.namespace("java")
//    }
//
//  }
//
//  private def findEnums(doc: Document, generator: ApacheJavaGenerator): util.List[TEnum] = {
//    val results = new util.ArrayList[TEnum]()
//
//    doc.enums.foreach(e => {
//      val controller = new EnumController(e, generator, getNameSpace(doc, language))
//
//      val tenum = new TEnum()
//      if (controller.has_namespace)
//        tenum.setNamespace(controller.namespace)
//
//      if (e.annotations.size > 0)
//        tenum.setAnnotations(e.annotations.map { case (key, value) => new Annotation(key, value) }.toList)
//
//      tenum.setName(controller.name)
//      tenum.setDoc(toDocString(e.docstring))
//      tenum.setEnumItems(new util.ArrayList[EnumItem]())
//
//      for (index <- (0 until controller.constants.size)) {
//        val enumFiled = controller.constants(index)
//
//        val name = enumFiled.name
//        val value = enumFiled.value.toString.toInt
//        val docString = toDocString(e.values(index).docstring)
//
//        val enumItem = new EnumItem()
//        enumItem.setLabel(name)
//        enumItem.setValue(value)
//        enumItem.setDoc(docString)
//
//        val teItem = e.values.get(index)
//        if (teItem.annotations.size > 0)
//          enumItem.setAnnotations(teItem.annotations.map { case (key, value) => new Annotation(key, value) }.toList)
//        tenum.getEnumItems.add(enumItem)
//      }
//
//      results.add(tenum)
//    })
//
//    results
//  }
//
//  private def findStructs(doc0: Document, generator: ApacheJavaGenerator): List[metadata.Struct] = {
//    doc0.structs.toList.map { (struct: StructLike) =>
//      val controller = new StructController(struct, false, generator, getNameSpace(doc0, language))
//
//      val domStruct = new metadata.Struct
//      domStruct.setNamespace(controller.namespace)
//      domStruct.setName(controller.name)
//      domStruct.setDoc(toDocString(struct.docstring))
//      if (struct.annotations.size > 0) {
//        domStruct.setAnnotations(struct.annotations.map { case (key, value) => new Annotation(key, value) }.toList)
//      }
//
//      val fields0 = controller.allFields.zip(controller.fields).toList.map { case (field, fieldController) =>
//        val tag0 = field.index.toString.toInt
//        val name0 = field.originalName
//        //val optional0 = fieldController.optional_or_nullable.toString.toBoolean
//        val optional0 = field.requiredness.isOptional
//        val docSrting0 = toDocString(field.docstring)
//        var dataType0: DataType = null
//
//        dataType0 = toDataType(field.fieldType, doc0, docSrting0)
//
//        val domField = new metadata.Field
//        domField.setTag(tag0)
//        domField.setName(name0)
//        domField.setOptional(optional0)
//        domField.setDoc(docSrting0)
//        domField.setDataType(dataType0)
//        domField.setPrivacy(false)
//
//        if (field.fieldAnnotations.size > 0) {
//          domField.setAnnotations(field.fieldAnnotations.map { case (key, value) => new Annotation(key, value) }.toList)
//        }
//        domField
//      }
//
//      domStruct.setFields(fields0)
//
//      domStruct
//    }
//
//  }
//
//
//  private def findServices(doc: Document, generator: ApacheJavaGenerator): util.List[metadata.Service] = {
//    val results = new util.ArrayList[metadata.Service]()
//
//    doc.services.foreach(s => {
//      val controller = new ServiceController(s, generator, getNameSpace(doc, language))
//
//      val service = new metadata.Service()
//
//
//      service.setNamespace(if (controller.has_namespace) controller.namespace else null)
//      service.setName(controller.name)
//      service.setDoc(toDocString(s.docstring))
//      if (s.annotations.size > 0)
//        service.setAnnotations(s.annotations.map { case (key, value) => new Annotation(key, value) }.toList)
//
//
//      val serviceVersion = if(service.annotations != null) service.annotations.find(item => {item.key.contains("version")}) else None
//      service.setMeta(new metadata.Service.ServiceMeta {
//        if (serviceVersion.nonEmpty) {
//          this.version = serviceVersion.get.value
//        } else {
//          this.version = "1.0.0"
//        }
//        this.timeout = 30000
//      })
//
//
//
//      val methods = new util.ArrayList[Method]()
//      for (tmpIndex <- (0 until controller.functions.size)) {
//        val functionField = controller.functions(tmpIndex)
//        //controller.functions.foreach(functionField => {
//        val request = new metadata.Struct()
//        val response = new metadata.Struct()
//
//        request.setName(functionField.name + "_args")
//        response.setName(functionField.name + "_result")
//
//        val method = new Method()
//        method.setName(functionField.name)
//        method.setRequest(request)
//        method.setResponse(response)
//        method.setDoc(toDocString(s.functions(tmpIndex).docstring))
//
//        if (s.functions(tmpIndex).annotations.size > 0)
//          method.setAnnotations(s.functions(tmpIndex).annotations.map { case (k, v) => new Annotation(k, v) }.toList)
//
//        if (method.getDoc != null && method.getDoc.contains("@IsSoaTransactionProcess"))
//          method.setSoaTransactionProcess(true)
//        else
//          method.setSoaTransactionProcess(false)
//
//        request.setFields(new util.ArrayList[metadata.Field]())
//        response.setFields(new util.ArrayList[metadata.Field]())
//
//        for (index <- (0 until functionField.fields.size)) {
//          val field = functionField.fields(index)
//
//          val realField = s.functions.get(tmpIndex).args.get(index)
//
//          val tag = index + 1
//          val name = field.field_name
//
//          val docSrting = if (realField.docstring.isDefined) toDocString(realField.docstring) else ""
//
//
//          val f = field.field_type.getClass.getDeclaredField("fieldType");
//          f.setAccessible(true)
//          val dataType = toDataType(f.get(field.field_type).asInstanceOf[FieldType], doc, docSrting)
//
//          val tfiled = new metadata.Field()
//          tfiled.setTag(tag)
//          tfiled.setName(name)
//          tfiled.setDoc(docSrting)
//          tfiled.setDataType(dataType)
//          tfiled.setOptional(field.optional)
//          if (realField.fieldAnnotations.size > 0)
//            tfiled.setAnnotations(realField.fieldAnnotations.map { case (k, v) => new Annotation(k, v) }.toList)
//          request.getFields.add(tfiled)
//        }
//
//        //val docSrting = if (s.functions.get(tmpIndex).docstring.isDefined) toDocString(s.functions.get(tmpIndex).docstring) else ""
//
//        val f = functionField.return_type.getClass.getDeclaredField("fieldType")
//        f.setAccessible(true)
//
//        //返回值没法获取注释，默认""
//        var dataType: DataType = null
//        if (f.get(functionField.return_type).getClass == com.twitter.scrooge.ast.Void.getClass) {
//          dataType = new DataType()
//          dataType.setKind(DataType.KIND.VOID)
//        } else {
//          dataType = toDataType(f.get(functionField.return_type).asInstanceOf[FieldType], doc, "")
//        }
//
//        val tfiled = new metadata.Field()
//        tfiled.setTag(0)
//        tfiled.setName("success")
//        tfiled.setDoc("")
//        tfiled.setDataType(dataType)
//        tfiled.setOptional(false)
//        response.getFields.add(tfiled)
//
//        methods.add(method)
//      }
//
//      service.setMethods(methods)
//
//      results.add(service)
//    })
//
//    results
//  }
//
//  def getAllStructs(resources: Array[String]): util.List[metadata.Struct] = {
//    resources.foreach(resource => {
//      val doc = generateDoc(resource)
//      docCache.put(resource.substring(resource.lastIndexOf(File.separator) + 1, resource.lastIndexOf(".")), doc)
//    })
//
//    docCache.values.foreach(doc => {
//      val generator = getGenerator(doc)
//      structCache.addAll(findStructs(doc, generator))
//    })
//    structCache.toList
//  }
//
//  def getAllEnums(resources: Array[String]): util.List[metadata.TEnum] = {
//    resources.foreach(resource => {
//      val doc = generateDoc(resource)
//      docCache.put(resource.substring(resource.lastIndexOf(File.separator) + 1, resource.lastIndexOf(".")), doc)
//    })
//
//    docCache.values.foreach(doc => {
//      val generator = getGenerator(doc)
//      enumCache.addAll(findEnums(doc, generator))
//    })
//    enumCache.toList
//  }
//
//  def toServices(resources: Array[String], serviceVersion: String): util.List[metadata.Service] = {
//    resources.foreach(resource => {
//      val doc = generateDoc(resource)
//
//      docCache.put(resource.substring(resource.lastIndexOf(File.separator) + 1, resource.lastIndexOf(".")), doc)
//    })
//
//    docCache.values.foreach(doc => {
//      val generator = getGenerator(doc)
//
//      enumCache.addAll(findEnums(doc, generator))
//      structCache.addAll(findStructs(doc, generator))
//      serviceCache.addAll(findServices(doc, generator))
//
//      for (enum <- enumCache)
//        mapEnumCache.put(enum.getNamespace + "." + enum.getName, enum)
//      for (struct <- structCache)
//        mapStructCache.put(struct.getNamespace + "." + struct.getName, struct)
//    })
//
//    for (index <- (0 until serviceCache.size())) {
//      val service = serviceCache.get(index)
//
//      val structSet = new util.HashSet[metadata.Struct]()
//      val enumSet = new util.HashSet[TEnum]()
//      //递归将service中所有method的所有用到的struct加入列表
//      val loadedStructs = new util.HashSet[String]()
//      for (method <- service.getMethods) {
//        for (field <- method.getRequest.getFields) {
//          getAllStructs(field.getDataType, structSet)
//          getAllEnums(field.getDataType, enumSet, loadedStructs)
//        }
//        for (field <- method.getResponse.getFields) {
//          getAllStructs(field.getDataType, structSet)
//          getAllEnums(field.getDataType, enumSet, loadedStructs)
//        }
//
//        if (method.annotations != null) {
//          method.annotations.foreach(annotation => getAllStructByAnnotation(annotation.getValue, structSet))
//        }
//
//      }
//      service.setStructDefinitions(structSet.toList)
//      service.setEnumDefinitions(enumSet.toList)
//      //      service.setEnumDefinitions(enumCache)
//      //      service.setStructDefinitions(structCache)
//    }
//    return serviceCache
//  }
//
//  /**
//    * 递归添加所有struct
//    *
//    * @param dataType
//    * @param structSet
//    */
//  def getAllStructs(dataType: metadata.DataType, structSet: java.util.HashSet[metadata.Struct]): Unit = {
//
//    if (dataType.getKind == DataType.KIND.STRUCT) {
//      val struct = mapStructCache.get(dataType.getQualifiedName)
//
//      if (structSet.contains(struct))
//        return
//
//      structSet.add(struct)
//
//      for (tmpField <- struct.getFields) {
//        getAllStructs(tmpField.getDataType, structSet)
//      }
//    }
//    else if (dataType.getKind == DataType.KIND.SET || dataType.getKind == DataType.KIND.LIST) {
//      getAllStructs(dataType.getValueType, structSet)
//
//    } else if (dataType.getKind == DataType.KIND.MAP) {
//      getAllStructs(dataType.getKeyType, structSet)
//      getAllStructs(dataType.getValueType, structSet)
//    }
//  }
//
//  /**
//    * qualifiedName:
//    *
//    * @param annoValue
//    * @param structSet
//    * @return
//    */
//  def getAllStructByAnnotation(annoValue: String, structSet: java.util.HashSet[metadata.Struct]) = {
//    annoValue.split(",").foreach(qualifiedName => {
//      val finalQualifiedName = if (language.equals("scala")) {
//        if (qualifiedName.contains(".")) {
//          toScalaNamespace(qualifiedName, 1)
//        } else qualifiedName
//      } else {
//        qualifiedName
//      }
//      val struct = mapStructCache.get(finalQualifiedName);
//      if (struct != null && !structSet.contains(struct)) {
//        structSet.add(struct)
//      }
//    })
//  }
//
//  /**
//    * 递归添加所有enum
//    *
//    * @param dataType
//    * @param enumSet
//    */
//  def getAllEnums(dataType: metadata.DataType, enumSet: util.HashSet[metadata.TEnum], loadedStructs: util.HashSet[String]): Unit = {
//
//    if (dataType.getKind == DataType.KIND.ENUM)
//      enumSet.add(mapEnumCache.get(dataType.getQualifiedName))
//
//    else if (dataType.getKind == DataType.KIND.STRUCT) {
//
//      if (loadedStructs.contains(dataType.getQualifiedName))
//        return
//
//      loadedStructs.add(dataType.getQualifiedName)
//
//      val struct = mapStructCache.get(dataType.getQualifiedName)
//
//      for (field <- struct.getFields)
//        getAllEnums(field.getDataType, enumSet, loadedStructs)
//    }
//    else if (dataType.getKind == DataType.KIND.SET || dataType.getKind == DataType.KIND.LIST) {
//      getAllEnums(dataType.getValueType, enumSet, loadedStructs)
//
//    } else if (dataType.getKind == DataType.KIND.MAP) {
//      getAllEnums(dataType.getKeyType, enumSet, loadedStructs)
//      getAllEnums(dataType.getValueType, enumSet, loadedStructs)
//    }
//  }
//}
