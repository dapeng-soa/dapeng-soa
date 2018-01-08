<%--
  Created by IntelliJ IDEA.
  User: craneding
  Date: 15/9/29
  Time: 下午4:01
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<header class="navbar navbar-fixed-top bs-docs-nav" id="top" role="banner">
  <div class="container">
    <div class="navbar-header">
      <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#navbar" aria-expanded="false" aria-controls="navbar">
        <span class="sr-only">Toggle navigation</span>
        <span class="icon-bar"></span>
      </button>
      <a href="${basePath}/" class="navbar-brand">快塑网技术平台</a>
    </div>
    <nav class="collapse navbar-collapse bs-navbar-collapse" id="navbar">
      <ul class="nav navbar-nav">
        <li class="${tagName == 'index' ? 'active' : ''}">
          <a href="${basePath}/">首页</a>
        </li>
        <!--
        <li class="${tagName == 'doc' ? 'active' : ''}">
          <a href="${basePath}/doc.htm">文档中心</a>
        </li>
        -->
        <li class="${tagName == 'api' ? 'active' : ''}">
          <a href="${basePath}/api/index.htm">API</a>
        </li>
        <!--
        <li class="${tagName == 'deploy' ? 'active' : ''}">
          <a href="${basePath}/deploy.htm">在线部署</a>
        </li>
        -->
        <!--
        <li class="${tagName == 'markdown' ? 'active' : ''}">
          <a href="${basePath}/markdown.htm">markdown</a>
        </li>
        -->
        <!--
        <li class="${tagName == 'contact' ? 'active' : ''}">
          <a href="${basePath}/contact.htm">关于</a>
        </li>
        -->
      </ul>

      <form action="${basePath}/search.htm" method="POST" class="navbar-form navbar-right" role="search">
        <div class="form-group">
          <input name="searchText" type="text" class="form-control" placeholder="">
        </div>
        <button type="submit" class="btn btn-default">搜索</button>
      </form>

    </nav>
  </div>
</header>
