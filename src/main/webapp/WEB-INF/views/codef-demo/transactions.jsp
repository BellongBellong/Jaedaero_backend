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
            <h1>입출금 거래내역</h1>
            <p class="account-number"><c:out value="${account}"/></p>
        </div>
        <a class="secondary-button link-button" href="${pageContext.request.contextPath}/codef-demo/accounts">계좌 목록으로</a>
    </header>

    <section class="transaction-card">
        <c:choose>
            <c:when test="${empty transactions}">
                <p class="empty-message">최근 3개월 내 거래내역이 없습니다.</p>
            </c:when>
            <c:otherwise>
                <table>
                    <thead>
                    <tr>
                        <th>일시</th>
                        <th>거래 내용</th>
                        <th>출금</th>
                        <th>입금</th>
                        <th>거래 후 잔액</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="transaction" items="${transactions}">
                        <tr>
                            <td><c:out value="${transaction.dateTime}"/></td>
                            <td><c:out value="${transaction.description}"/></td>
                            <td class="withdrawal"><c:out value="${transaction.withdrawal}"/></td>
                            <td class="deposit"><c:out value="${transaction.deposit}"/></td>
                            <td><c:out value="${transaction.balance}"/></td>
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
