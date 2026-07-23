<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>제대로 · 거래내역</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/codef-demo.css">
</head>
<body>
<main class="page-shell dashboard-shell">
    <header class="dashboard-header">
        <div>
            <p class="eyebrow">${bank.displayName} · 최근 3개월</p>
            <h1>${transactionKind == 'INSTALLMENT_SAVINGS' ? '적금 납입내역' : '입출금 거래내역'}</h1>
            <p class="account-number"><c:out value="${accountDisplay}"/></p>
        </div>
        <a class="secondary-button link-button" href="${pageContext.request.contextPath}/codef-demo/accounts">계좌 목록으로</a>
    </header>

    <form class="period-filter" action="${pageContext.request.contextPath}/codef-demo/transactions" method="post">
        <input type="hidden" name="accountId" value="${accountId}">
        <input type="hidden" name="transactionKind" value="${transactionKind}">
        <label for="startDate">조회 시작일</label>
        <input id="startDate" type="date" name="startDate" value="${startDate}">
        <label for="endDate">조회 종료일</label>
        <input id="endDate" type="date" name="endDate" value="${endDate}">
        <button class="secondary-button" type="submit">기간 조회</button>
    </form>

    <section class="transaction-list">
        <c:choose>
            <c:when test="${empty transactions}">
                <p class="empty-message">최근 3개월 내 거래내역이 없습니다.</p>
            </c:when>
            <c:otherwise>
                <c:forEach var="transaction" items="${transactions}">
                    <article class="transaction-item ${transaction.deposit ? 'deposit-card' : 'withdrawal-card'}">
                        <div class="transaction-topline">
                            <span class="transaction-type">
                                <c:choose>
                                    <c:when test="${transaction.deposit}">입금</c:when>
                                    <c:otherwise>출금</c:otherwise>
                                </c:choose>
                            </span>
                            <strong class="transaction-amount">
                                <c:choose>
                                    <c:when test="${transaction.deposit}">+<c:out value="${transaction.amount}"/></c:when>
                                    <c:otherwise>-<c:out value="${transaction.amount}"/></c:otherwise>
                                </c:choose>
                            </strong>
                        </div>
                        <h2 class="transaction-description"><c:out value="${transaction.description}"/></h2>
                        <div class="transaction-meta">
                            <span><c:out value="${transaction.date}"/></span>
                            <span><c:out value="${transaction.time}"/></span>
                            <span>거래 후 잔액 <strong><c:out value="${transaction.balance}"/></strong></span>
                        </div>
                    </article>
                </c:forEach>
            </c:otherwise>
        </c:choose>
    </section>
</main>
</body>
</html>
