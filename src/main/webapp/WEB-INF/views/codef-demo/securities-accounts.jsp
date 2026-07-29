<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ko"><head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>제대로 · 주식 계좌 조회</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/codef-demo.css">
</head><body>
<main class="page-shell dashboard-shell securities-dashboard">
    <header class="dashboard-header account-dashboard-header"><div class="dashboard-title"><p class="eyebrow"><c:out value="${institutionDisplayName}"/> 연동 완료</p><h1>주식 계좌 조회</h1><p>계좌를 선택한 뒤 전체 자산 또는 보유 주식 잔고를 확인하세요.</p></div><form action="${pageContext.request.contextPath}/codef-demo/disconnect" method="post"><button class="secondary-button disconnect-button" type="submit">다른 기관 연결</button></form></header>
    <c:if test="${not empty errorMessage}"><p class="alert error"><c:out value="${errorMessage}"/></p></c:if>
    <form class="unified-assets-form" action="${pageContext.request.contextPath}/codef-demo/all-assets" method="post"><button class="primary-button" type="submit">모든 연동 계좌 자산 한 번에 조회</button></form>
    <c:choose><c:when test="${empty accounts}"><section class="empty-card">조회된 주식 계좌가 없습니다.</section></c:when><c:otherwise><section class="securities-query-list"><c:forEach var="item" items="${accounts}"><article class="securities-query-card"><div><span class="available">증권 조회</span><h2><c:out value="${item.accountName}"/></h2><p class="account-number"><c:out value="${item.accountDisplay}"/></p><p class="asset-label">평가 자산</p><p class="asset-amount"><c:out value="${item.balance}"/></p></div><div class="securities-query-actions"><a class="query-link primary-query-link" href="${pageContext.request.contextPath}/codef-demo/securities/assets?accountId=${item.accountId}">전체 자산 조회</a><a class="query-link" href="${pageContext.request.contextPath}/codef-demo/securities/holdings?accountId=${item.accountId}">주식 잔고 조회</a></div></article></c:forEach></section></c:otherwise></c:choose>
</main></body></html>
