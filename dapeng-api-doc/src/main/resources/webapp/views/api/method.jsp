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
    <script src="${basePath}/js/api/method.js"></script>
    <script src="${basePath}/js/api/struct.js"></script>
    <script src="${basePath}/js/api/enum.js"></script>
    <script>
        $(function () {
            var mAction = new api.MethodAction();

            mAction.findMethod("${service.name}", "${service.meta.version}", "${method.name}",isModel=true);
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
            <li><a class="active">${method.name}</a></li>
        </ol>
    </div>
    <div class="row">
        <div class="col-sm-3 col-md-3">
            <div class="list-group">
                <c:forEach var="m" items="${methods}">
                    <a class="list-group-item ${m == method ? 'active' : ''}"
                       href="${basePath}/api/method/${service.name}/${service.meta.version}/${m.name}.htm">
                        <span class="glyphicon glyphicon-chevron-right"></span>
                        <c:out value="${m.name}"/>
                    </a>
                </c:forEach>
            </div>
        </div>
        <div class="col-sm-9 col-md-9">
            <div class="page-header mt5">
                <h1 class="mt5">${method.name}</h1>
            </div>
            <button type="button" class="btn btn-info" onclick="window.location.href = '${basePath}/api/test/${service.name}/${service.meta.version}/${method.name}.htm'">在线测试</button>
            <p data-marked-id="marked">${method.doc}</p>

            <h3>坐标</h3>
            <table class="table table-bordered">
                <thead>
                <tr class="breadcrumb">
                    <th>服务名</th>
                    <th>版本号</th>
                    <th>方法名</th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td>${service.namespace}.${service.name}</td>
                    <td>${service.meta.version}</td>
                    <td>${method.name}</td>
                </tr>
                </tbody>
            </table>

            <h3>输入参数</h3>
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
                <c:forEach var="field" items="${method.request.fields}" varStatus="vs">
                    <tr>
                        <td>${field.tag}</td>
                        <td>${field.name}</td>
                        <td class="req-field-datatype-${vs.index}"></td>
                        <td>${field.optional == true ? '否' : '是'}</td>
                        <td data-marked-id="marked">${field.doc}</td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>

            <h3>返回结果</h3>
            <table class="table table-bordered">
                <thead>
                <tr class="breadcrumb">
                    <th>类型</th>
                    <th>描述</th>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="field" items="${method.response.fields}" varStatus="vs">
                    <tr>
                        <td class="resp-field-datatype-${vs.index}"></td>
                        <td data-marked-id="marked">${field.doc}</td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>

            <h3>事件清单</h3>
            <table class="table table-bordered">
                <thead>
                <tr class="breadcrumb">
                    <th>#</th>
                    <th>事件</th>
                    <%--<th>简述</th>--%>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="event" items="${events}" varStatus="vs">
                    <tr>
                        <td>${vs.index + 1}</td>

                        <td>
                            <a href="javascript:void(0)"
                               onclick=getStructDetail1('${service.name}','${service.meta.version}','${event.event}')>${event.shortName}</a>
                        </td>

                        <%--<td data-marked-id="marked">${anEnum.mark}</td>--%>
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
