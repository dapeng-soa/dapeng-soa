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
    <script src="${basePath}/js/api/struct.js"></script>
    <script src="${basePath}/js/api/enum.js"></script>
    <script>
        $(function () {
            var sAction = new api.StructAction();

            sAction.findStruct("${service.name}", "${service.meta.version}", "${struct.namespace}.${struct.name}");
        });
    </script>
</head>
<body>
<jsp:include page="../core/struct-model.jsp"/>
<jsp:include page="../core/scroll-top.jsp"/>
<jsp:include page="../core/header.jsp"/>

<div class="bs-docs-content container">
    <div class="row mt5">
        <ol class="breadcrumb">
            <li><a href="${basePath}/">首页</a></li>
            <li><a href="${basePath}/api/index.htm">API</a></li>
            <li><a href="${basePath}/api/service/${service.name}/${service.meta.version}.htm">${service.name}</a></li>
            <li><a class="active">${struct.name}</a></li>
        </ol>
    </div>
    <div class="row">
        <div class="col-sm-3 col-md-3">
            <div class="list-group">
                <c:forEach var="s" items="${structs}">
                    <a class="list-group-item ${s == struct ? 'active' : ''}" href="${basePath}/api/struct/${service.name}/${service.meta.version}/${s.namespace}.${s.name}.htm" style="overflow: hidden;
    text-overflow: ellipsis;">
                        <span class="glyphicon glyphicon-chevron-right"></span>
                        <c:out value="${s.name}"/>
                    </a>
                </c:forEach>
            </div>
        </div>
        <div class="col-sm-9 col-md-9">
            <div class="page-header mt5">
                <h1 class="mt5">${struct.name}</h1>
            </div>
            <p data-marked-id="marked">${struct.doc}</p>

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
                    <td>${struct.namespace}.${struct.name}</td>
                </tr>
                </tbody>
            </table>

            <h3>数据成员</h3>
            <table class="table table-bordered">
                <thead>
                <tr class="breadcrumb">
                    <th>#</th>
                    <th>名称</th>
                    <th>类型</th>
                    <th>是否必填</th>
                    <th>描述</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="field" items="${struct.fields}" varStatus="vs">
                    <tr>
                        <td>${field.tag}</td>
                        <td>${field.name}</td>
                        <td class="struct-field-datatype-${vs.index}"></td>
                        <td>${field.optional == true ? '<span class="label label-success">否</code>' : '<span class="label label-danger">是</span>'}</td>
                        <td data-marked-id="marked">${field.doc}</td>
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
