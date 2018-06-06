package com.github.dapeng.maven.plugin.util

import java.io.{File, FileInputStream, PrintWriter}
import java.sql.{Connection, DriverManager, ResultSet}
import java.text.ParseException
import java.util.Properties

import scala.collection.mutable

object DbGeneratorUtil {

  val driver = "com.mysql.jdbc.Driver"
  val enumRegx = """(.*\s*),(\s*\d:\s*.*\(\s*[\d|a-zA-Z|_]+\)\s*;?)+""".r
  val singleEnumRegx = """\s*([\d]+):\s*([\u4e00-\u9fa5|\w|-]+)\(([\d|a-zA-Z|_]+)\)""".r

  def generateEntityFile(fileContent: String, targetPath: String, fileName: String) = {
    val file = new File(targetPath + fileName)
    val created = if (!file.getParentFile.exists()) file.getParentFile.mkdirs() else true
    println(s"generating file: ${targetPath}${file.getName}: ${created}")
    val printWriter = new PrintWriter(file,"UTF-8")
    printWriter.println(fileContent)
    printWriter.flush()
    printWriter.close()
  }

  def getEnumFields(columnName: String, columnComment: String) = {
    val result = columnComment match {
      case enumRegx(a, b) =>
        val enums: Array[(String, String)] = b.split(";").map(item => {
          item match {
            case singleEnumRegx(index, cnChars, enChars) =>

              //              println(s"foundEnumValue ${columnName} =>  index: ${index}  cnChars:${cnChars}  enChars: ${enChars}")
              (index, enChars)
            case _ => throw new ParseException(s"invalid enum format: ${columnName} -> ${item} should looks like Int:xxx(englishWord)", 0)
          }
        })
        enums.toList
      case _ =>
        //println(s" Not match enum comment, skipped....${columnComment}")
        List[(String, String)]()
    }
    result
  }

  def generateEnumFile(tableName: String, columnName: String, columnComment: String, targetPath: String, packageName: String, fileName: String) = {
    val enumFields = getEnumFields(columnName, columnComment)
    if (enumFields.size > 0) {
      println(s"foundEnumValue ${columnName} =>  size = ${enumFields.size}")
      generateEntityFile(toEnumFileTemplate(tableName, enumFields, packageName, columnName), targetPath, fileName)
    }
  }


  def toDbClassTemplate(tableName: String, packageName: String, columns: List[(String, String, String, String)]) = {
    val sb = new StringBuilder(256)
    val className = toFirstUpperCamel(tableNameConvert(tableName))
    sb.append(s" package ${packageName}.entity \r\n")

    //如果有枚举字段，需要引入
    if (columns.exists(column => !getEnumFields(column._1, column._3).isEmpty)) {
      sb.append(s" import ${packageName}.enum._ \r\n")
    }

    if (columns.exists(c => List("DATETIME", "DATE", "TIMESTAMP").contains(c._2))) {
      sb.append(" import java.sql.Timestamp \r\n")
    }

    sb.append(" import wangzx.scala_commons.sql.ResultSetMapper \r\n\r\n ")

    sb.append(s" case class ${className} ( \r\n")
    columns.foreach(column => {
      val hasValidEnum: Boolean = !getEnumFields(column._1, column._3).isEmpty
      sb.append(s" /** ${column._3} */ \r\n")
      sb.append(toCamel(keywordConvert(column._1))).append(": ").append(
        if (hasValidEnum) toFirstUpperCamel(tableNameConvert(tableName)) + toFirstUpperCamel(column._1) else toScalaFieldType(column._2, column._4)
      ).append(",\r\n")
    })
    if (sb.contains(",")) sb.delete(sb.lastIndexOf(","), sb.lastIndexOf(",") + 1)
    sb.append(") \t\n \t\n")

    //添加数据库的隐式转换
    /*
    *  object TableName {
    *     implicit val resultSetMapper: ResultSetMapper[TableName] = ResultSetMapper.meterial[TableName]
    *  }
    *
    * */
    sb.append(s" object ${className} { \r\n")
    sb.append(s" \timplicit val resultSetMapper: ResultSetMapper[${className}] = ResultSetMapper.material[${className}] \r\n")
    sb.append(" }")

    sb.toString()

  }


  /**
    * EnumClass content:
    * class ${enumClassName} private(val id:Int, val name:String) extends DbEnum
    * object ${enumClassName} {
    * val NEW = new ${enumClassName}(0, "NEW")
    * def unknowne(id: Int) = new OrderStatus(id, s"<$id>")
    * def valueOf(id: Int): OrderStatus = id match {
    * case 0 => NEW
    * case _ => unknowne(id)
    * }
    * implicit object Accessor extends DbEnumJdbcValueAccessor[OrderStatus](valueOf)
    * }
    *
    * @param enums
    * @return
    */
  def toEnumFileTemplate(tableName: String, enums: List[(String, String)], packageName: String, columnName: String): String = {
    val sb = new StringBuilder(256)
    val enumClassName = toFirstUpperCamel(tableNameConvert(tableName)) + toFirstUpperCamel(columnName)
    sb.append(s" package ${packageName}.enum \r\n")
    sb.append(" import wangzx.scala_commons.sql.DbEnum \r\n")
    sb.append(" import wangzx.scala_commons.sql._ \r\n")

    /*
    override def toString(): String = s"(${id},${name})"

	 override def equals(obj: Any): Boolean =
		 if (obj == null) false
		 else if (obj == this) true
		 else if (obj.isInstanceOf[AccountType]) obj.asInstanceOf[AccountType].id == this.id
		 else false

	 override def hashCode(): Int = this.id
     */
    sb.append(s" class ${enumClassName} private(val id:Int, val name:String) extends DbEnum { \r\n")
    sb.append(s""" \t override def toString(): String = "(" + id + "," + name + ")"  \r\n\r\n """)
    sb.append(s"\t override def equals(obj: Any): Boolean = { \r\n")
    sb.append(s" \t\t\t if (obj == null) false \r\n")
    sb.append(s" \t\t\t else if (obj.isInstanceOf[${enumClassName}]) obj.asInstanceOf[${enumClassName}].id == this.id \r\n")
    sb.append(s" \t\t\t else false \r\n")
    sb.append(s" \t } \r\n\r\n")
    sb.append(" \t override def hashCode(): Int = this.id \r\n")
    sb.append(" } \r\n\r\n")

    sb.append(s" object ${enumClassName} { \r\n")
    enums.foreach(enum => {
      //val enumUpper = enum._2.toCharArray.map(i => if (i.isUpper) s"_${i}" else i.toUpper.toString).mkString("")
      val enumUpper = enum._2.toUpperCase
      println(s" enumUpper: ${enumUpper}")
      sb.append(s"""\t val ${enumUpper} = new ${enumClassName}(${enum._1},"${enumUpper}") \r\n""")
    })
    sb.append(s"""\t def unknown(id: Int) = new ${enumClassName}(id, id+"") \r\n""")

    sb.append(s"""\t def valueOf(id: Int): ${enumClassName} = id match { \r\n""")
    enums.foreach(enum => {
      //val enumUpper = enum._2.toCharArray.map(i => if (i.isUpper) s"_${i}" else i.toUpper.toString).mkString("")
      val enumUpper = enum._2.toUpperCase
      sb.append(s" \t\t case ${enum._1} => ${enumUpper} \r\n")
    })
    sb.append(" \t\t case _ => unknown(id) \r\n")
    sb.append(" } \r\n")

    sb.append(s" def apply(v: Int) = valueOf(v) \r\n")
    sb.append(s" def unapply(v: ${enumClassName}): Option[Int] = Some(v.id) \r\n")

    sb.append(s" implicit object Accessor extends DbEnumJdbcValueAccessor[${enumClassName}](valueOf) \r\n")

    sb.append("}")

    sb.toString()
  }

  def getTableColumnInfos(tableName: String, db: String, connection: Connection): List[(String, String, String, String)] = {

    val columns: ResultSet = connection.getMetaData.getColumns("", db, tableName, "")

    val columnInfos = mutable.MutableList[(String, String, String, String)]()
    while (columns.next()) {
      val columnName = columns.getString("COLUMN_NAME")
      val columnDataType = columns.getString("TYPE_NAME")
      val columnComment = columns.getString("REMARKS")
      val columnNullable = columns.getString("IS_NULLABLE")
      val columnInfo = (columnName, columnDataType, columnComment, columnNullable)
      columnInfos += columnInfo
    }

    columnInfos.toList
  }


  def keywordConvert(word: String) = {
    if (List("abstract",
      "case",
      "catch",
      "class",
      "def",
      "do",
      "else",
      "extends",
      "false",
      "final",
      "finally",
      "for",
      "forSome",
      "if",
      "implicit",
      "import",
      "lazy",
      "macro",
      "match",
      "new",
      "null",
      "object",
      "override",
      "package",
      "private",
      "protected",
      "return",
      "sealed",
      "super",
      "this",
      "throw",
      "trait",
      "try",
      "true",
      "type",
      "val",
      "var",
      "while",
      "with",
      "yield").exists(_.equals(word))) {
      s"`${word}`"
    } else word
  }

  /**
    * sss_xxx => sssXxx
    *
    * @param name
    * @return
    */
  def toCamel(name: String): String = {
    val camel = name.split("_").map(item => {
      val result = item.toLowerCase
      result.charAt(0).toUpper + result.substring(1)
    }).mkString("")
    camel.replaceFirst(s"${camel.charAt(0)}", s"${camel.charAt(0).toLower}")
  }

  /**
    * aaa_bbb => AaaBbb
    *
    * @param name
    * @return
    */
  def toFirstUpperCamel(name: String): String = {
    name.split("_").map(item => {
      val result = item.toLowerCase
      result.charAt(0).toUpper + result.substring(1)
    }).mkString("")
  }

  def toScalaFieldType(tableFieldType: String, isNullable: String): String = {
    val dataType = tableFieldType.toUpperCase() match {
      case "INT" | "SMALLINT" | "TINYINT" | "INT UNSIGNED" | "SMALLINT UNSIGNED" | "TINYINT UNSIGNED" | "BIT" => "Int"
      case "BIGINT" => "Long"
      case "CHAR" | "VARCHAR" => "String"
      case "DECIMAL" | "DOUBLE" | "FLOAT" => "BigDecimal"
      case "DATETIME" | "DATE" | "TIMESTAMP" => "Timestamp"
      case "ENUM" | "TEXT" => "String"
      case "LONGBLOB" | "BLOB" | "MEDIUMBLOB" => "Array[Byte]"
      case _ => throw new ParseException(s"tableFieldType = ${tableFieldType} 无法识别", 1023)
    }
    if (isNullable.equals("YES")) {
      s"Option[${dataType}]"
    } else {
      dataType
    }
  }


  def connectJdbc(): Option[Connection] = {
    val url = System.getProperty("db.url")
    val user = System.getProperty("db.user")
    val passwd = System.getProperty("db.password")
    println(s"connectTo, url: ${url}, user: ${user}, passwd: ${passwd}")

    //val url = s"jdbc:mysql://${ip}/${db}?useUnicode=true&characterEncoding=utf8"
    try {
      if (url == null || url.isEmpty) throw new Exception("please check if 'plugin.db.url' property is config in dapeng.properties ")
      if (user == null || user.isEmpty) throw new Exception("please check if 'plugin.db.user' property is config in dapeng.properties ")
      if (passwd == null || passwd.isEmpty) throw new Exception("please check if 'plugin.db.password' property is config in dapeng.properties ")

      Class.forName(driver)
      Some(DriverManager.getConnection(url, user, passwd))
    } catch {
      case e: Exception =>
        println(s" failed to instance jdbc driver: ${e.getCause} , ${e.getMessage}")
        Option.empty
    }

  }


  def getTableNamesByDb(db: String, connection: Connection) = {
    //    val sql = s"select table_name from information_schema.tables where table_schema='${db}' and table_type='base table'";
    //
    //    val sqlStatement = connection.prepareStatement(sql)
    //    val resultSet = sqlStatement.executeQuery()
    //    val tableNames = mutable.MutableList[String]()
    //    while (resultSet.next()) {
    //      val tableName = resultSet.getString("table_name")
    //      tableNames += tableName
    //    }

    val tables = connection.getMetaData.getTables("", db, "", null)
    val tableNames = mutable.MutableList[String]()
    while (tables.next()) {
      tableNames += tables.getString("TABLE_NAME")
    }

    tableNames
  }

  def tableNameConvert(tableName: String): String = {
    if (tableName.endsWith("ies")) tableName.substring(0, tableName.length - 3) + "y"
    else if (tableName.endsWith("ses")) tableName.substring(0, tableName.length - 3) + "s"
    else if (tableName.endsWith("shes")) tableName.substring(0, tableName.length - 4) + "sh"
    else if (tableName.endsWith("ches")) tableName.substring(0, tableName.length - 4) + "ch"
    else if (tableName.endsWith("xes")) tableName.substring(0, tableName.length - 3) + "x"
    else if (tableName.endsWith("ves")) tableName.substring(0, tableName.length - 3) + "f?"
    else if (tableName.endsWith("s")) tableName.substring(0, tableName.length - 1)
    else tableName
  }

  def generateDbClass(tableName: String, db: String, connection: Connection, packageName: String, baseTargetPath: String): Unit = {

    val separator = System.getProperty("file.separator")
    val columns = getTableColumnInfos(tableName.toLowerCase, db, connection)
    val dbClassTemplate = toDbClassTemplate(tableName, packageName, columns)
    val targetPath = baseTargetPath + "src"+separator+"main"+ separator +"scala"+ separator + packageName.split("\\.").mkString(separator) + separator

    generateEntityFile(dbClassTemplate, targetPath + "entity/", s"${toFirstUpperCamel(tableNameConvert(tableName))}.scala")

    columns.foreach(column => {
      generateEnumFile(tableName, column._1, column._3, targetPath + "enum/", packageName, s"${toFirstUpperCamel(tableNameConvert(tableName)) + toFirstUpperCamel(column._1)}.scala")
    })
  }

  def loadSystemProperties(file: File): Unit = {
    if (file.canRead) {
      val properties = new Properties()
      properties.load(new FileInputStream(file))
      import scala.collection.JavaConversions._
      val results = properties.keySet().map(_.toString)
      results.foreach(keyString => {
        System.setProperty(keyString, properties.getProperty(keyString))
      })
    }
  }


}
