/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.dapeng.code.generator

import java.util

import com.github.dapeng.core.metadata.DataType.KIND
import com.github.dapeng.core.metadata.TEnum.EnumItem
import com.github.dapeng.core.metadata._

import scala.collection.mutable.ArrayBuffer
import scala.xml.{Atom, Elem, Node, Text}

/**
  * 代码生成器
 *
  * @author craneding
  * @date 15/7/23
  */
abstract class CodeGenerator {


  val XML_REGEN_COUNT = 5
  val lt = "<"
  val gt = ">"
  val and = "&"

  def generate(services: util.List[Service], outDir: String)

  def generate(services: util.List[Service], outDir: String, generateAll:Boolean, structs:util.List[Struct], enums:util.List[TEnum])

  protected def toStructArrayBuffer(array: util.List[Struct]): ArrayBuffer[Struct] = {
    val newArray: ArrayBuffer[Struct] = ArrayBuffer()

    for (index <- (0 until array.size())) {
      newArray += array.get(index)
    }

    return newArray
  }

  protected def toFieldArrayBuffer(array: util.List[Field]): ArrayBuffer[Field] = {
    val newArray: ArrayBuffer[Field] = ArrayBuffer()

    for (index <- (0 until array.size())) {
        newArray += array.get(index)
    }
    newArray
  }

  protected def toTEnumArrayBuffer(array: util.List[TEnum]): ArrayBuffer[TEnum] = {
    val newArray: ArrayBuffer[TEnum] = ArrayBuffer()

    for (index <- (0 until array.size())) {
      newArray += array.get(index)
    }

    return newArray
  }

  protected def toEnumItemArrayBuffer(array: util.List[EnumItem]): ArrayBuffer[EnumItem] = {
    val newArray: ArrayBuffer[EnumItem] = ArrayBuffer()

    for (index <- (0 until array.size())) {
      newArray += array.get(index)
    }

    return newArray
  }

  protected def toMethodArrayBuffer(array: util.List[Method]): ArrayBuffer[Method] = {
    val newArray: ArrayBuffer[Method] = ArrayBuffer()

    for (index <- (0 until array.size())) {
      newArray += array.get(index)
    }

    return newArray
  }

  protected def toNameSpaceArrayBuffer(array: util.Set[String]): ArrayBuffer[String] = {
    val newArray: ArrayBuffer[String] = ArrayBuffer()

    val itrs = array.iterator();
    while (itrs.hasNext) {
      val itr = itrs.next();

      newArray += itr
    }

    return newArray
  }

  class StringTemplate(elem: Elem) {
    val builder = new StringBuilder

    formatNode(elem)

    def appendToBuilder(text: String) {
      builder.append(text)
    }

    def formatNode(node: Node): Unit = node match {
      case Text(text) => appendToBuilder(text)
      case x: Atom[_] =>
        appendToBuilder(x.data.toString)
      case b @ <div>{ _* }</div> =>
        formatDiv(b.asInstanceOf[Elem])
      case b @ <span>{ _* }</span> =>
        b.child.foreach(formatNode)
      case b @ <block>{ _* }</block> =>
        formatBlock(b.asInstanceOf[Elem])
    }

    def formatDiv(elem: Elem) {
      elem.child.foreach { node => formatNode(node) }
    }

    def formatBlock(elem: Elem) {
      appendToBuilder("{")
      elem.child.foreach { node =>
        formatNode(node)
      }
      appendToBuilder("}")
    }

    override def toString = builder.toString()
  }

}
