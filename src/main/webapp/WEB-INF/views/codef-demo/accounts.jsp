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
            <p class="eyebrow"><c:out value="${institutionDisplayName}"/></p>
            <h1>연결된 보유계좌</h1>
            <p>입출금 계좌 카드를 선택하면 최근 3개월 거래내역을 확인할 수 있습니다.</p>
        </div>
        <form action="${pageContext.request.contextPath}/codef-demo/disconnect" method="post">
            <button class="secondary-button" type="submit">연결 해제</button>
        </form>
    </header>

    <c:if test="${not empty errorMessage}">
        <p class="alert error"><c:out value="${errorMessage}"/></p>
    </c:if>

    <c:if test="${not securitiesInstitution}">
    <section class="savings-status ${militarySavingsStatus.active ? 'active' : militarySavingsStatus.matured ? 'matured' : 'not-found'}">
        <div>
            <p class="eyebrow">장병내일준비적금 조회</p>
            <h2>
                <c:choose>
                    <c:when test="${militarySavingsStatus.active}">복무 중 적금 가입 상태</c:when>
                    <c:when test="${militarySavingsStatus.matured}">적금 만기 상태</c:when>
                    <c:otherwise>적금 미확인</c:otherwise>
                </c:choose>
            </h2>
            <p><c:out value="${militarySavingsStatus.message}"/></p>
        </div>
        <c:if test="${not empty militarySavingsStatus.savingsAccounts}">
            <div class="savings-list">
                <c:forEach var="saving" items="${militarySavingsStatus.savingsAccounts}">
                    <article class="saving-item">
                        <strong><c:out value="${saving.accountName}"/></strong>
                        <span><c:out value="${saving.accountDisplay}"/> · <c:out value="${saving.balance}"/></span>
                        <span>만기일: <c:out value="${saving.maturityDate}"/>
                            <c:if test="${saving.matured}">(만기)</c:if>
                        </span>
                    </article>
                </c:forEach>
            </div>
        </c:if>
        <p class="savings-note">※ 은행 계좌를 모두 연동하지 않은 경우에는 실제 복무 여부를 확정할 수 없습니다.</p>
    </section>
    </c:if>

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
                                <c:when test="${item.savingsTransaction}"><span class="available">납입 조회</span></c:when>
                                <c:when test="${item.transactionSupported}"><span class="available">거래 조회</span></c:when>
                                <c:otherwise><span class="unavailable">조회 준비 중</span></c:otherwise>
                            </c:choose>
                        </div>
                        <h2><c:out value="${item.accountName}"/></h2>
                        <p class="account-number"><c:out value="${item.accountDisplay}"/></p>
                        <p class="balance"><c:out value="${item.balance}"/></p>
                        <c:if test="${item.transactionSupported}">
                            <form action="${pageContext.request.contextPath}/codef-demo/transactions" method="post">
                                <input type="hidden" name="accountId" value="${item.accountId}">
                                <input type="hidden" name="transactionKind" value="${item.transactionKind}">
                                <button class="card-button" type="submit">
                                    <c:choose>
                                        <c:when test="${item.savingsTransaction}">최근 3개월 납입내역 보기</c:when>
                                        <c:otherwise>최근 3개월 거래내역 보기</c:otherwise>
                                    </c:choose>
                                </button>
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
