<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>제대로 · 거래내역</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/codef-demo.css?v=20260731">
</head>
<body>
<main class="page-shell dashboard-shell">
    <header class="dashboard-header">
        <div>
            <p class="eyebrow"><c:out value="${institutionDisplayName}"/> · 최근 3개월</p>
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
                        <div class="transaction-category-editor">
                            <label for="category-${transaction.transactionId}">카테고리</label>
                            <select id="category-${transaction.transactionId}" class="transaction-category-select"
                                    data-transaction-id="${transaction.transactionId}" data-current-value="${transaction.category}">
                                <option value="SALARY" ${transaction.category == 'SALARY' ? 'selected' : ''}>급여</option>
                                <option value="ASSET" ${transaction.category == 'ASSET' ? 'selected' : ''}>자산</option>
                                <option value="PX" ${transaction.category == 'PX' ? 'selected' : ''}>PX</option>
                                <option value="FOOD" ${transaction.category == 'FOOD' ? 'selected' : ''}>식비</option>
                                <option value="SHOPPING" ${transaction.category == 'SHOPPING' ? 'selected' : ''}>쇼핑</option>
                                <option value="TRANSPORT" ${transaction.category == 'TRANSPORT' ? 'selected' : ''}>교통</option>
                                <option value="LEISURE" ${transaction.category == 'LEISURE' ? 'selected' : ''}>여가</option>
                                <option value="MEDICAL" ${transaction.category == 'MEDICAL' ? 'selected' : ''}>의료</option>
                                <option value="ETC" ${transaction.category == 'ETC' ? 'selected' : ''}>기타</option>
                            </select>
                            <span class="category-save-status" aria-live="polite"></span>
                        </div>
                    </article>
                </c:forEach>
            </c:otherwise>
        </c:choose>
    </section>
</main>
<script>
    (function () {
        const contextPath = '${pageContext.request.contextPath}';
        const demoUserId = ${demoUserId};

        document.querySelectorAll('.transaction-category-select').forEach(function (select) {
            select.addEventListener('change', async function () {
                const status = select.parentElement.querySelector('.category-save-status');
                const previousValue = select.dataset.currentValue;
                select.disabled = true;
                status.className = 'category-save-status';
                status.textContent = '저장 중…';
                try {
                    const response = await fetch(
                        contextPath + '/api/v1/transactions/' + encodeURIComponent(select.dataset.transactionId) + '/category',
                        {
                            method: 'PUT',
                            headers: {
                                'Content-Type': 'application/json',
                                'X-User-Id': String(demoUserId)
                            },
                            body: JSON.stringify({category: select.value})
                        }
                    );
                    if (!response.ok) {
                        throw new Error((await response.text()) || '카테고리를 저장하지 못했습니다.');
                    }
                    select.dataset.currentValue = select.value;
                    status.classList.add('success');
                    status.textContent = '저장됨';
                } catch (error) {
                    select.value = previousValue;
                    status.classList.add('error');
                    status.textContent = '저장 실패';
                } finally {
                    select.disabled = false;
                }
            });
        });
    }());
</script>
</body>
</html>
