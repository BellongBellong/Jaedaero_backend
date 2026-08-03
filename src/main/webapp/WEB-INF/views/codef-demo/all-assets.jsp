<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>제대로 · 전체 자산</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/codef-demo.css?v=20260731">
</head>
<body>
<main class="page-shell dashboard-shell asset-overview-shell">
    <header class="asset-overview-header">
        <div>
            <p class="eyebrow">자산 현황</p>
            <h1>내 전체 자산</h1>
            <p><c:out value="${unifiedAssets.refreshedAccountCount}"/>개 연동 계좌의 저장된 조회 결과예요.</p>
        </div>
        <a class="secondary-button link-button back-button" href="${pageContext.request.contextPath}/codef-demo/accounts">
            ← 계좌 목록
        </a>
    </header>

    <section class="asset-summary-card">
        <div class="asset-summary-copy">
            <span>총 보유 자산</span>
            <strong><c:out value="${unifiedAssets.totalAmount}"/></strong>
            <p>대출 계좌는 총자산 계산에서 제외했어요.</p>
        </div>
        <div class="asset-summary-meta">
            <span>연동 완료</span>
            <strong><c:out value="${unifiedAssets.refreshedAccountCount}"/>개 계좌</strong>
        </div>
    </section>

    <section class="institution-asset-groups">
        <c:forEach var="group" items="${unifiedAssets.institutionGroups}">
            <section class="institution-asset-group">
                <div class="institution-group-heading">
                    <div class="institution-mark">₩</div>
                    <h2><c:out value="${group.institutionName}"/></h2>
                </div>
                <div class="unified-account-grid">
                    <c:forEach var="item" items="${group.accounts}">
                        <article class="unified-account-card">
                            <div class="unified-account-topline">
                                <span class="account-kind ${item.securities ? 'securities' : 'bank'}">
                                    <c:out value="${item.securities ? '증권 계좌' : '은행 계좌'}"/>
                                </span>
                                <span class="asset-status"><c:out value="${item.statusMessage}"/></span>
                            </div>
                            <div class="unified-account-info">
                                <h3><c:out value="${item.accountName}"/></h3>
                                <p class="account-number"><c:out value="${item.accountDisplay}"/></p>
                            </div>
                            <div class="unified-amount">
                                <span>현재 자산</span>
                                <strong><c:out value="${item.assetAmount}"/></strong>
                            </div>
                            <c:choose>
                                <c:when test="${item.securities}">
                                    <a class="card-detail-button" href="${pageContext.request.contextPath}/codef-demo/securities/holdings?accountId=${item.accountId}">
                                        주식 내역 보기 <span>→</span>
                                    </a>
                                </c:when>
                                <c:when test="${item.bankDetailAvailable}">
                                    <form action="${pageContext.request.contextPath}/codef-demo/transactions" method="post">
                                        <input type="hidden" name="accountId" value="${item.accountId}">
                                        <input type="hidden" name="transactionKind" value="${item.transactionKind}">
                                        <button class="card-detail-button" type="submit">세부 내역 보기 <span>→</span></button>
                                    </form>
                                </c:when>
                                <c:otherwise>
                                    <span class="card-detail-unavailable">세부 내역은 준비 중이에요</span>
                                </c:otherwise>
                            </c:choose>
                        </article>
                    </c:forEach>
                </div>
            </section>
        </c:forEach>
    </section>
</main>
</body>
</html>
