<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core_rt" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <jsp:include page="../core/resource.jsp"/>
    <link rel="stylesheet" href="${basePath}/css/styles/monokai_sublime.css">
    <script src="https://cdn.bootcss.com/highlight.js/8.8.0/highlight.min.js"></script>
    <script src="${basePath}/js/formatmarked.js"></script>
</head>
<body>
<jsp:include page="../core/header.jsp"/>

<div class="bs-docs-content container">
    <div class="row mt5">
        <ol class="breadcrumb">
            <li><a href="${basePath}/">首页</a></li>
            <li><a href="${basePath}/api/index.htm">API</a></li>
            <li><a class="active">${service.name}</a></li>
        </ol>
    </div>
    <div class="row">
        <div class="col-sm-3 col-md-3">
            <div class="list-group">
                <c:forEach var="s" items="${services}">
                    <a class="list-group-item ${s == service ? 'active' : ''}"
                       href="${basePath}/api/service/${s.name}/${s.meta.version}.htm">
                        <span class="glyphicon glyphicon-tree-deciduous"></span>
                        <c:choose>
                            <c:when test="${empty s.doc}">
                                <c:out value="${s.name}"/>
                            </c:when>
                            <c:otherwise>
                                <c:out value="${s.doc}"/>
                            </c:otherwise>
                        </c:choose>
                    </a>
                </c:forEach>
            </div>
        </div>
        <div class="col-sm-9 col-md-9">
            <div class="page-header mt5">
                <h1 class="mt5">${service.doc}
                    <small>${service.name}</small>
                </h1>
            </div>

            <h3>坐标</h3>
            <table class="table table-bordered">
                <thead>
                <tr>
                    <th>服务名</th>
                    <th>版本号</th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td>${service.namespace}.${service.name}</td>
                    <td>${service.meta.version}</td>
                </tr>
                </tbody>
            </table>

            <h3>方法列表</h3>
            <table class="table table-bordered">
                <thead>
                <tr>
                    <th>#</th>
                    <th>方法名列表</th>
                    <th>简述</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="method" items="${service.methods}" varStatus="vs">
                    <tr>
                        <td>${vs.index + 1}</td>
                        <td>
                            <a href="${basePath}/api/method/${service.name}/${service.meta.version}/${method.name}.htm">${method.name}</a>
                        </td>
                        <td data-marked-id="marked">${method.doc}</td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>

            <h3>结构体列表</h3>
            <table class="table table-bordered">
                <thead>
                <tr>
                    <th>#</th>
                    <th>结构体列表</th>
                    <th>简述</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="struct" items="${service.structDefinitions}" varStatus="vs">
                    <tr>
                        <td>${vs.index + 1}</td>
                        <td>
                            <a href="${basePath}/api/struct/${service.name}/${service.meta.version}/${struct.namespace}.${struct.name}.htm">${struct.name}</a>
                        </td>
                        <td data-marked-id="marked">${struct.doc}</td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>

            <h3>枚举结构列表</h3>
            <table class="table table-bordered">
                <thead>
                <tr>
                    <th>#</th>
                    <th>枚举结构列表</th>
                    <th>简述</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="anEnum" items="${service.enumDefinitions}" varStatus="vs">
                    <tr>
                        <td>${vs.index + 1}</td>
                        <td>
                            <a href="${basePath}/api/enum/${service.name}/${service.meta.version}/${anEnum.namespace}.${anEnum.name}.htm">${anEnum.name}</a>
                        </td>
                        <td data-marked-id="marked">${anEnum.doc}</td>
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
