<%--
  Created by IntelliJ IDEA.
  User: craneding
  Date: 15/9/29
  Time: 下午1:12
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    request.setAttribute("basePath", request.getContextPath());
%>
<meta charset="utf-8"/>
<%
    if (request.getAttribute("DEFAULT_TITLE") == null) {
%>
<title>大鹏Api文档</title>
<%
    } else {
%>
<title>大鹏Api文档</title>
<%
    }
%>
<meta name="author" content="大鹏开源">
<meta name="description" content="大鹏Api文档，是一个大鹏内部的接口文档站点！">
<meta name="keywords" content="api,thrift,zookeeper,netty,redis,mysql">
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
<meta name="format-detection" content="telephone=no">
<meta name="format-detection" content="email=no">

<!-- 新 Bootstrap 核心 CSS 文件 -->
<link rel="stylesheet" href="//cdn.bootcss.com/bootstrap/3.3.5/css/bootstrap.min.css">
<!-- 可选的Bootstrap主题文件（一般不用引入） -->
<link rel="stylesheet" href="//cdn.bootcss.com/bootstrap/3.3.5/css/bootstrap-theme.min.css">
<link rel="stylesheet" href="${basePath}/css/default.css" type="text/css"/>
<link rel="stylesheet" href="${basePath}/css/comment.css" type="text/css"/>
<!-- jQuery文件。务必在bootstrap.min.js 之前引入 -->
<script src="//cdn.bootcss.com/jquery/1.11.3/jquery.min.js"></script>
<!-- 最新的 Bootstrap 核心 JavaScript 文件 -->
<script src="//cdn.bootcss.com/bootstrap/3.3.5/js/bootstrap.min.js"></script>
<!-- marked -->
<script src="//cdn.bootcss.com/marked/0.3.5/marked.min.js"></script>
<script>
    window.basePath = "${basePath}";
</script>