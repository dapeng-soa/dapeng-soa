<%--
  Created by IntelliJ IDEA.
  User: craneding
  Date: 15/9/29
  Time: 下午2:10
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" isErrorPage="true" %>
<html lang="zh-CN">
<head>
    <%
        request.setAttribute("DEFAULT_TITLE", "未找到页面");
    %>
    <jsp:include page="../core/resource.jsp"/>
</head>
<body>
<div class="container">
    <div class="page-header">
        <h1>系统错误</h1>
    </div>
</div>
</body>
</html>
