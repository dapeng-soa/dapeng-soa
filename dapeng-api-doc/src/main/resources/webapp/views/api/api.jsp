<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jstl/core_rt" %>
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <jsp:include page="../core/resource.jsp"/>
</head>
<body>
<jsp:include page="../core/header.jsp"/>

<%
    String[] colors = new String[]{"#6e5698", "#2b69ac", "#3fa77a", "#c64249", "#4d97d9", "#db8539", "#4daeb5", "#b581ae"};
    int index = 0;
%>
<div class="bs-docs-content container">
    <div class="row mt5">
        <ol class="breadcrumb">
            <li><a href="${basePath}/">首页</a></li>
            <li><a class="active">API</a></li>
        </ol>
    </div>
    <div class="row">
        <div class="m10">
            <c:forEach var="service" items="${services}">
                <div class="col-sm-6 col-md-4 col-lg-3">
                    <div style="height: 300px;" class="thumbnail">
                        <a href="${basePath}/api/service/${service.name}/${service.meta.version}.htm" title="${service.doc}">
                            <div style="width:100%; height:100px; background:<%=colors[index % colors.length]%>; color:white; text-align:center; font-size:20px; line-height:100px;">
                                <span style="font-weight: bold; font-size: 28px;"><%=index + 1%></span>
                                <c:choose>
                                    <c:when test="${empty service.doc}">
                                        <c:out value="${service.name}"/>
                                    </c:when>
                                    <c:otherwise>
                                        <c:out value="${service.doc}"/>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                        </a>

                        <div style="text-align:center;" class="caption">
                            <h3 style="font-size:20px;">
                                <a href="${basePath}/api/service/${service.name}/${service.meta.version}.htm" title="${service.doc}">
                                    <c:out value="${service.name}"/> <br>
                                    <small>版本：<c:out value="${service.meta.version}"/></small>
                                </a>
                            </h3>
                            <blockquote>
                                <p>method：<code>${service.methods.size()}</code><br>enum：<code>${service.enumDefinitions.size()}</code><br>struct：<code>${service.structDefinitions.size()}</code></p>
                            </blockquote>
                        </div>
                    </div>
                </div>
                <% index++; %>
            </c:forEach>
        </div>
    </div>
</div>

<jsp:include page="../core/footer.jsp"/>
</body>
</html>
