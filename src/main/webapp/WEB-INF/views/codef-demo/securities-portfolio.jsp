<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>제대로 · 증권 정보</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/codef-demo.css">
</head>
<body>
<main class="page-shell dashboard-shell">
    <header class="dashboard-header">
        <div>
            <p class="eyebrow"><c:out value="${institutionDisplayName}"/> · <c:out value="${portfolio.accountDisplay}"/></p>
            <h1>${stockOnly ? '주식 잔고' : '전체 자산'}</h1>
            <p>예수금 <strong><c:out value="${portfolio.depositAmount}"/></strong></p>
        </div>
        <a class="secondary-button link-button" href="${pageContext.request.contextPath}/codef-demo/accounts">계좌 목록으로</a>
    </header>

    <section class="transaction-card">
        <c:choose>
            <c:when test="${empty portfolio.holdings}">
                <p class="empty-message">조회된 ${stockOnly ? '주식 잔고' : '자산 정보'}가 없습니다.</p>
            </c:when>
            <c:otherwise>
                <table>
                    <thead>
                    <tr>
                        <th>유형</th><th>상품·종목</th><th>보유 수량</th><th>매입 금액</th><th>평가 금액</th><th>평가 손익</th><th>수익률</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="item" items="${portfolio.holdings}">
                        <tr>
                            <td><c:out value="${item.productType}"/></td>
                            <td><strong><c:out value="${item.itemName}"/></strong><br><span class="table-muted"><c:out value="${item.itemCode}"/> · <c:out value="${item.currency}"/></span></td>
                            <td><c:out value="${item.quantity}"/></td>
                            <td><c:out value="${item.purchaseAmount}"/></td>
                            <td><c:out value="${item.valuationAmount}"/></td>
                            <td><c:out value="${item.valuationProfit}"/></td>
                            <td><c:out value="${item.earningsRate}"/>%</td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </c:otherwise>
        </c:choose>
    </section>
</main>
</body>
</html>
