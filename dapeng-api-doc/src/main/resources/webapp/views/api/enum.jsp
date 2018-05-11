<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core_rt" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <jsp:include page="../core/resource.jsp"/>
    <link rel="stylesheet" href="${basePath}/css/styles/monokai_sublime.css">
    <script src="${basePath}/js/highlight/8.8.0/highlight.min.js"></script>
    <script src="${basePath}/js/formatmarked.js"></script>
    <script src="${basePath}/js/api/model.js"></script>
</head>
<body>
<jsp:include page="../core/scroll-top.jsp"/>
<jsp:include page="../core/header.jsp"/>

<div class="bs-docs-content container">
    <div class="row mt5">
        <ol class="breadcrumb">
            <li><a href="${basePath}/">首页</a></li>
            <li><a href="${basePath}/api/index.htm">API</a></li>
            <li><a href="${basePath}/api/service/${service.name}/${service.meta.version}.htm">${service.name}</a></li>
            <li><a class="active">${anEnum.name}</a></li>
        </ol>
    </div>
    <div class="row">
        <div class="col-sm-3 col-md-3">
            <div class="list-group">
                <c:forEach var="e" items="${enums}">
                    <a class="list-group-item ${e == anEnum ? 'active' : ''}" href="${basePath}/api/enum/${service.name}/${service.meta.version}/${e.namespace}.${e.name}.htm">
                        <span class="glyphicon glyphicon-chevron-right"></span>
                        <c:out value="${e.name}"/>
                    </a>
                </c:forEach>
            </div>
        </div>
        <div class="col-sm-9 col-md-9">
            <div class="page-header mt5">
                <h1 class="mt5">${anEnum.name}</h1>
            </div>
            <p data-marked-id="marked">${anEnum.doc}</p>

            <h3>坐标</h3>
            <table class="table table-bordered">
                <thead>
                <tr class="breadcrumb">
                    <th>服务名</th>
                    <th>版本号</th>
                    <th>结构体全限定名</th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td>${service.name}</td>
                    <td>${service.meta.version}</td>
                    <td>${anEnum.namespace}.${anEnum.name}</td>
                </tr>
                </tbody>
            </table>

            <h3>枚举成员</h3>
            <table class="table table-bordered">
                <thead>
                <tr class="breadcrumb">
                    <th>#</th>
                    <th>名称</th>
                    <th>值</th>
                    <th>描述</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="e" items="${anEnum.enumItems}" varStatus="vs">
                    <tr>
                        <td>${vs.index + 1}</td>
                        <td>${e.label}</td>
                        <td>${e.value}</td>
                        <td data-marked-id="marked">${e.doc}</td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>

        </div>
    </div>
</div>

<jsp:include page="../core/footer.jsp"/>
</body>
</html>
