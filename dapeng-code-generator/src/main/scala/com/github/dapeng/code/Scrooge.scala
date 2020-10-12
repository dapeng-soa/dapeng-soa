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
 package com.github.dapeng.code

import java.io.{File, FileNotFoundException, FilenameFilter}
import javax.annotation.processing.FilerException

import com.github.dapeng.code.generator.ScalaGenerator

//import com.github.dapeng.code.generator.scala.ScalaGenerator
import com.github.dapeng.code.generator.{JavaGenerator, JavascriptGenerator, JsonGenerator, MetadataGenerator}
import com.github.dapeng.code.parser.ThriftCodeParser

/**
  * @author craneding
  * @date 15/5/11
  */
object Scrooge {

  val help =
    """-----------------------------------------------------------------------
      | args: -gen metadata,js,json file
      | Scrooge [options] file
      | Options:
      |   -out dir    Set the output location for generated files.
      |   -gen STR    Generate code with a dynamically-registered generator.
      |               STR has the form language[val1,val2,val3].
      |               Keys and values are options passed to the generator.
      |   -v version  Set the version of the Service generated.
      |   -in dir     Set input location of all Thrift files.
      |   -all        Generate all structs and enums
      |
      | Available generators (and options):
      |   metadata
      |   scala
      |   java
      |-----------------------------------------------------------------------
    """.stripMargin

  def main(args: Array[String]) {

    println(s"scrooge:${args.mkString(" ")}")

    if (args.length <= 0) {
      println(help);
      return
    }

    lazy val outDir: String = getParameterByName(args, "-out", Option(System.getProperty("user.dir")))
    lazy val inDir: String = getParameterByName(args, "-in", Some(""))
    lazy val language: String = getParameterByName(args, "-gen")
    lazy val version: String = getParameterByName(args, "-v",Some(""))
    lazy val generateAll: Boolean = getParameterExist(args, "-all")
    lazy val isHelp: Boolean = getParameterExist(args, "-help")
    lazy val isDelete: Boolean = !getParameterExist(args, "-notDel")
    lazy val groupId: String = getParameterByName(args, "-groupId", Some(""))
    lazy val artifactId: String = getParameterByName(args, "-artifactId", Some(""))
    lazy val modelVersion: String = getParameterByName(args, "-modelVersion", Some(""))

    lazy val thriftFiles: Array[File] = getResourceFilePathArray(args, inDir)
    lazy val resources = thriftFiles.map(_.getAbsolutePath)



    if (isHelp) {
      println(help)
      return
    }

    println(s"groupId=${groupId}artifactId=${artifactId}")
    checkAndCreate(outDir)
    checkAndCreate(inDir)


    try {
      //根据时间戳判断是否需要重新生成文件
      //1. 如果 (thrift文件 + outDirFiles) 任一修改时间 > xml的时间 => needUpdate
      //2. 如果没有xml文件 => needUpdate
      //3. 如果language == scala && scala文件没有生成过 => needUpdate
      //4. 如果language == java && java文件没有生成过 => needUpdate
      val resourcePath = new File(resources(0)).getParentFile.getParentFile
      if (!resourcePath.isDirectory) {
        resourcePath.mkdir()
      }
      val needUpdate = {
        val xmlFiles = resourcePath.listFiles().filter(_.getName.endsWith(".xml"))
        val targetDirFiles = getFiles(outDir).filter(file=> {
          val fileName = file.getName
          fileName.endsWith(".java") || fileName.endsWith(".scala")
        })

        if (xmlFiles.length <= 0) {
          true
        } else if ((xmlFiles.toList ++: targetDirFiles)
          .exists(generatedFile => thriftFiles.exists(_.lastModified() > generatedFile.lastModified()))) {
          true
        } else {
          language match {
            case "java" => if (targetDirFiles.count(_.getName.endsWith(".java")) <= 0) true else false
            case "scala" => if (targetDirFiles.count(_.getName.endsWith(".scala")) <= 0) true else false
          }
        }
      }


      if (thriftFiles.nonEmpty && language != "" && needUpdate) {
        //删除文件再生成
        if (isDelete) {
          fileDel(new File(outDir), language)
        }
        val parserLanguage = if (language == "scala") "scala" else "java"
        val services = new ThriftCodeParser(parserLanguage).toServices(resources, version,groupId,artifactId,modelVersion)
        val structs = if (generateAll) new ThriftCodeParser(parserLanguage).getAllStructs(resources) else null
        val enums = if (generateAll) new ThriftCodeParser(parserLanguage).getAllEnums(resources) else null

        language match {
          case "metadata" => new MetadataGenerator().generate(services, outDir)
          case "js" => new JavascriptGenerator().generate(services, outDir)
          case "json" => new JsonGenerator().generate(services, outDir)
          case "java" => new JavaGenerator().generate(services, outDir, generateAll, structs, enums)
          case "scala" => new ScalaGenerator().generate(services, outDir, generateAll, structs, enums)
        }

      } else if (resources == null || language == "") {
        throw new RuntimeException("resources is null or language is null")
      }
    }
    catch {
      case e: Exception =>
        e.printStackTrace()

        println(s"Error: ${e.getMessage}")
        throw e
    }
  }

  private def getResourceFilePathArray(args: Array[String], inDir: => String) = {
    if (!inDir.isEmpty) {

      new File(inDir).listFiles(new FilenameFilter {
        override def accept(dir: File, name: String): Boolean = name.endsWith(".thrift")
      })

    } else {
      //获取到resource
      val filesArray = args(args.length - 1).split(",")
      filesArray.map(filePath => {
        val file = new File(filePath)
        if (!file.exists())
          throw new FileNotFoundException(s"File[$filePath] is not found")
        else if (new File(filePath).isDirectory || !filePath.endsWith(".thrift"))
          throw new FilerException(s"File[$filePath] is not a *.thrift")
        file
      })
    }
  }

  private def checkAndCreate(outDir: String): Unit = {
    val file = new File(outDir)
    if (!file.exists())
      file.mkdirs()

    if (file.isFile) {
      file.delete()
      throw new FilerException(s"File[$outDir] is not a directory")
    }
  }

  private def getParameterByName(args: Array[String], parameterName: String, defaultValue: Option[String] = None) = {
    val indexOpt = args.zipWithIndex.find(_._1.equals(parameterName)).map(_._2)

    assert(indexOpt.isDefined || defaultValue.isDefined, s"$parameterName 参数不存在")
    indexOpt match {
      case Some(index) =>
        assert(args.length > index + 1, "")
        args(index + 1)
      case _ => defaultValue.get
    }

  }

  private def getParameterExist(args: Array[String], parameterName: String) = {
    args.exists(_.equals(parameterName))
  }

  def failed(): Unit = {

  }

  def getFiles(path: String): List[File] = {
    if (new File(path).isDirectory) {
      new File(path).listFiles().flatMap(i => getFiles(i.getPath)).toList
    } else {
      List(new File(path))
    }
  }

  //删除目录和文件
  def fileDel(path: File, language: String) {
    if (!path.exists())
      return
    if (path.isFile && (path.getName.endsWith(language) || path.getName.endsWith(".xml"))) {
      path.delete()
      return
    }

    val file: Array[File] = path.listFiles()
    if (file != null) {
      for (d <- file) {
        fileDel(d, language)
      }
    }

  }

}
