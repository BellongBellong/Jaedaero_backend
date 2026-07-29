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
<main class="page-shell dashboard-shell ${securitiesInstitution ? 'securities-dashboard' : ''}">
    <header class="dashboard-header account-dashboard-header">
        <div class="dashboard-title">
            <p class="eyebrow"><c:out value="${institutionDisplayName}"/> 연동 완료</p>
            <h1>${securitiesInstitution ? '내 증권 자산' : '연결된 보유계좌'}</h1>
            <p>
                <c:choose>
                    <c:when test="${securitiesInstitution}">계좌별 전체 자산과 보유 주식의 평가 현황을 확인하세요.</c:when>
                    <c:otherwise>입출금·적금 계좌의 잔액과 최근 거래내역을 확인하세요.</c:otherwise>
                </c:choose>
            </p>
        </div>
        <form action="${pageContext.request.contextPath}/codef-demo/disconnect" method="post">
            <button class="secondary-button disconnect-button" type="submit">다른 기관 연결</button>
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
            <section class="account-grid ${securitiesInstitution ? 'securities-account-grid' : ''}">
                <c:forEach var="item" items="${accounts}">
                    <article class="account-card ${securitiesInstitution ? 'securities-account-card' : item.transactionSupported ? 'clickable' : 'disabled'}">
                        <div class="card-topline">
                            <span class="category"><c:out value="${securitiesInstitution ? '증권 계좌' : item.category}"/></span>
                            <c:choose>
                                <c:when test="${securitiesInstitution}"><span class="available">증권 조회</span></c:when>
                                <c:when test="${item.savingsTransaction}"><span class="available">납입 조회</span></c:when>
                                <c:when test="${item.transactionSupported}"><span class="available">거래 조회</span></c:when>
                                <c:otherwise><span class="unavailable">조회 준비 중</span></c:otherwise>
                            </c:choose>
                        </div>
                        <h2><c:out value="${item.accountName}"/></h2>
                        <p class="account-number"><c:out value="${item.accountDisplay}"/></p>
                        <div class="account-balance-block">
                            <span>${securitiesInstitution ? '평가 자산' : '현재 잔액'}</span>
                            <p class="balance"><c:out value="${item.balance}"/></p>
                        </div>
                        <c:choose>
                        <c:when test="${securitiesInstitution}">
                            <div class="securities-actions">
                                <a class="card-button link-button" href="${pageContext.request.contextPath}/codef-demo/securities/assets?accountId=${item.accountId}">
                                    전체 자산 보기 →
                                </a>
                                <a class="card-button secondary-card-button link-button" href="${pageContext.request.contextPath}/codef-demo/securities/holdings?accountId=${item.accountId}">
                                    주식 잔고 보기 →
                                </a>
                            </div>
                        </c:when>
                        <c:when test="${item.transactionSupported}">
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
                        </c:when>
                        </c:choose>
                    </article>
                </c:forEach>
            </section>
        </c:otherwise>
    </c:choose>
</main>
</body>
</html>
