<%@ page import="com.github.dapeng.doc.SearchController" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <jsp:include page="../core/resource.jsp"/>
</head>
<body>
<jsp:include page="../core/header.jsp"/>

<div class="bs-docs-content container">
    <div class="row mt5">
        <ol class="breadcrumb">
            <li><a href="${basePath}/">首页</a></li>
            <li><a href="${basePath}/api/index.htm">API</a></li>
            <li><a class="active">搜索</a></li>
        </ol>
    </div>
    <div class="row">
        <div class="col-sm-3 col-md-3">
            <div class="list-group">
                <c:forEach var="s" items="${services}">
                    <a class="list-group-item ${s == service ? 'active' : ''}" href="${basePath}/api/service/${s.name}/${s.meta.version}.htm">
                        <span class="glyphicon glyphicon-tree-deciduous"></span>
                        <c:choose>
                            <c:when test="${empty s.doc}">
                                <c:out value="${s.name}API"/>
                            </c:when>
                            <c:otherwise>
                                <c:out value="${s.doc}API"/>
                            </c:otherwise>
                        </c:choose>
                    </a>
                </c:forEach>
            </div>
        </div>
        <div class="col-sm-9 col-md-9">

            <%
                List<SearchController.SearchResultItem> resultItems = (List<SearchController.SearchResultItem>)request.getAttribute("resultList");
                if(resultItems.size() == 0) {%>
            <h3>没有搜到结果，请重新输入再试</h3>
            <%
            } else {
            %>

            <div class="list-group">

                <c:forEach var="resultItem" items="${resultList}">
                    <a href="${basePath}/${resultItem.url}" class="list-group-item">
                        <h4 class="list-group-item-heading">${resultItem.name}</h4>
                        <p class="list-group-item-text">${resultItem.url}</p>
                    </a>
                </c:forEach>
            </div>

            <%
                }
            %>

        </div>
    </div>
</div>

<jsp:include page="../core/footer.jsp"/>
</body>
</html>