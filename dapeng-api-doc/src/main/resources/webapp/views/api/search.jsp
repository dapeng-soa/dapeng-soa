<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core_rt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <jsp:include page="../core/resource.jsp"/>
</head>
<body>
<jsp:include page="../core/scroll-top.jsp"/>
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
                    <a class="list-group-item ${s == service ? 'active' : ''}"
                       href="${basePath}/api/service/${s.name}/${s.meta.version}.htm">
                        <span class="glyphicon glyphicon-chevron-right"></span>
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
            <p>
                <span class="glyphicon glyphicon-globe"></span> 服务&nbsp;&nbsp;
                <span class="glyphicon glyphicon-flash"></span> 接口&nbsp;&nbsp;
                <span class="glyphicon glyphicon-retweet"></span> 结构体&nbsp;&nbsp;
                <span class="glyphicon glyphicon-list-alt"></span> 枚举&nbsp;&nbsp;
            </p>
            <c:choose>
                <c:when test="${empty resultList}">
                    <h3>没有搜到结果，请重新输入再试</h3>
                </c:when>
                <c:otherwise>
                    <div class="list-group">
                        <c:forEach var="resultItem" items="${resultList}">
                            <a href="${basePath}/${resultItem.url}" class="list-group-item">
                                <span style="position: absolute;right:20px;top:40%;" class="
                                <c:choose>
                                    <c:when test="${fn:contains(resultItem.url,'api/service')}">
                                        glyphicon glyphicon-globe
                                    </c:when>
                                    <c:when test="${fn:contains(resultItem.url,'api/method')}">
                                        glyphicon glyphicon-flash
                                    </c:when>
                                    <c:when test="${fn:contains(resultItem.url,'api/struct')}">
                                        glyphicon glyphicon-retweet
                                    </c:when>
                                    <c:otherwise>
                                        glyphicon glyphicon-list-alt
                                    </c:otherwise>

                                </c:choose>
                                "></span>
                                <h4 class="list-group-item-heading">${resultItem.name}</h4>
                                <p class="list-group-item-text">${resultItem.url}</p>
                            </a>
                        </c:forEach>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</div>

<jsp:include page="../core/footer.jsp"/>
</body>
</html>