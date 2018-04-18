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

    var outDir: String = null
    var inDir: String = null
    var resources: Array[String] = null //the thrift files
    var language: String = ""
    var version: String = null
    var generateAll: Boolean = false
    var isDelete: Boolean = true

    try {
      for (index <- 0 until args.length) {

        args(index) match {
          case "-gen" =>
            //获取languages
            if (index + 1 < args.length) language = args(index + 1)
          case "-out" =>
            //获取到outDir
            if (index + 1 < args.length) outDir = args(index + 1)
            val file = new File(outDir)
            if (!file.exists())
              file.createNewFile()

            if (file.isFile) {
              file.delete()
              throw new FilerException(s"File[${outDir}] is not a directory")
            }
          case "-in" =>
            if (index + 1 < args.length) inDir = args(index + 1)
            val file = new File(inDir)
            if (!file.exists())
              file.createNewFile()

            if (file.isFile) {
              file.delete()
              throw new FilerException(s"File[${inDir}] is not a directory")
            }
          case "-help" => println(help); return
          case "-v" => if (index + 1 < args.length) version = args(index + 1)
          case "-all" => generateAll = true
          case "-notDel" => isDelete = false
          case _ =>
        }
      }

      if (outDir == null) // 如果输出路径为空,则默认为当前目录
        outDir = System.getProperty("user.dir")


      if (inDir != null) {

        resources = new File(inDir).listFiles(new FilenameFilter {
          override def accept(dir: File, name: String): Boolean = name.endsWith(".thrift")
        }).map(file => file.getAbsolutePath)

      } else {
        //获取到resource
        resources = args(args.length - 1).split(",")
        resources.foreach { str =>
          val file = new File(str)
          if (!file.exists())
            throw new FileNotFoundException(s"File[${str}] is not found")
          else if (new File(str).isDirectory || !str.endsWith(".thrift"))
            throw new FilerException(s"File[${str}] is not a *.thrift")
        }
      }

      //根据时间戳判断是否需要重新生成文件
      //1. 如果 (thrift文件 + outDirFiles) 任一修改时间 > xml的时间 => needUpdate
      //2. 如果没有xml文件 => needUpdate
      //3. 如果language == scala && scala文件没有生成过 => needUpdate
      //4. 如果language == java && java文件没有生成过 => needUpdate
      val resourcePath = new File(outDir + System.getProperty("file.separator") + "resources")
      if (!resourcePath.isDirectory){
        resourcePath.mkdir()
      }
      val thriftFiles = resources.map(new File(_))
      val needUpdate = {
        if(resourcePath.isDirectory) {
          val xmlFiles = resourcePath.listFiles().filter(_.getName.endsWith(".xml"))
          val targetDirFiles = getFiles(outDir).filter(file => {
            val fileName = file.getName
            fileName.endsWith(".java") || fileName.endsWith(".scala")
          })

          if (xmlFiles.size <= 0) {
            true
          } else if ((xmlFiles.toList ++: targetDirFiles)
            .exists(generatedFile => thriftFiles.exists(_.lastModified() > generatedFile.lastModified()))) {
            true
          } else {
            language match {
              case "java" => if (targetDirFiles.filter(_.getName.endsWith(".java")).size <= 0) true else false
              case "scala" => if (targetDirFiles.filter(_.getName.endsWith(".scala")).size <= 0) true else false
            }
          }
        }else true
      }

      if (resources != null && language != "" && needUpdate) {
        //删除文件再生成
        if (isDelete) {
          fileDel(new File(outDir), language)
        }
        val parserLanguage = if (language == "scala") "scala" else "java"
        val services = new ThriftCodeParser(parserLanguage).toServices(resources, version)
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
    }
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
  def fileDel(path: File , language : String) {
    if (!path.exists())
      return
    if (path.isFile() && (path.getName.endsWith(language)  || path.getName.endsWith(".xml"))) {
      path.delete()
      return
    }

    val file: Array[File] = path.listFiles()
    if (file != null) {
      for (d <- file) {
        fileDel(d , language)
      }
    }

  }

}
