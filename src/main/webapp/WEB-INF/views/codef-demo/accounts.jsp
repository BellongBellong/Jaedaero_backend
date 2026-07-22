<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>제대로 · 보유계좌</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/codef-demo.css">
</head>
<body>
<main class="page-shell dashboard-shell">
    <header class="dashboard-header">
        <div>
            <p class="eyebrow">${bank.displayName}</p>
            <h1>연결된 보유계좌</h1>
            <p>입출금 계좌 카드를 선택하면 최근 3개월 거래내역을 확인할 수 있습니다.</p>
        </div>
        <form action="${pageContext.request.contextPath}/codef-demo/disconnect" method="post">
            <button class="secondary-button" type="submit">연결 해제</button>
        </form>
    </header>

    <c:choose>
        <c:when test="${empty accounts}">
            <section class="empty-card">조회된 보유계좌가 없습니다.</section>
        </c:when>
        <c:otherwise>
            <section class="account-grid">
                <c:forEach var="item" items="${accounts}">
                    <article class="account-card ${item.transactionSupported ? 'clickable' : 'disabled'}">
                        <div class="card-topline">
                            <span class="category"><c:out value="${item.category}"/></span>
                            <c:choose>
                                <c:when test="${item.transactionSupported}"><span class="available">거래 조회</span></c:when>
                                <c:otherwise><span class="unavailable">조회 준비 중</span></c:otherwise>
                            </c:choose>
                        </div>
                        <h2><c:out value="${item.accountName}"/></h2>
                        <p class="account-number"><c:out value="${item.accountDisplay}"/></p>
                        <p class="balance"><c:out value="${item.balance}"/></p>
                        <c:if test="${item.transactionSupported}">
                            <form action="${pageContext.request.contextPath}/codef-demo/transactions" method="post">
                                <input type="hidden" name="account" value="${item.account}">
                                <button class="card-button" type="submit">최근 3개월 거래내역 보기</button>
                            </form>
                        </c:if>
                    </article>
                </c:forEach>
            </section>
        </c:otherwise>
    </c:choose>
</main>
</body>
</html>
