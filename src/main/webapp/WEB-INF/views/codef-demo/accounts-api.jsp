<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>제대로 · 연동 계좌 조회</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/codef-demo.css">
</head>
<body>
<main class="page-shell dashboard-shell">
    <header class="dashboard-header account-dashboard-header">
        <div class="dashboard-title">
            <p class="eyebrow">${institutionDisplayName} 연동 완료</p>
            <h1>연동 계좌 조회</h1>
            <p>공개 계좌 API에서 조회한 계좌 목록입니다.</p>
        </div>
        <form action="${pageContext.request.contextPath}/codef-demo/disconnect" method="post">
            <button class="secondary-button disconnect-button" type="submit">다른 기관 연결</button>
        </form>
    </header>
    <form class="unified-assets-form" action="${pageContext.request.contextPath}/codef-demo/all-assets" method="post">
        <button class="primary-button" type="submit">모든 연동 계좌 자산 한 번에 조회</button>
    </form>
    <p id="accountLoading" class="form-note">연동 계좌를 불러오는 중입니다.</p>
    <p id="accountError" class="alert error" hidden></p>
    <section id="accountGrid" class="account-grid" hidden></section>
</main>
<script>
    (async function () {
        const contextPath = '${pageContext.request.contextPath}';
        const userId = ${demoUserId};
        const organizationCode = '${organizationCode}';
        const businessType = '${businessType}';
        const refresh = ${refresh};
        const loading = document.getElementById('accountLoading');
        const error = document.getElementById('accountError');
        const grid = document.getElementById('accountGrid');
        const currency = new Intl.NumberFormat('ko-KR', {style: 'currency', currency: 'KRW', maximumFractionDigits: 0});

        function appendText(element, className, value) {
            const child = document.createElement('p');
            child.className = className;
            child.textContent = value;
            element.appendChild(child);
        }

        function transactionButton(account) {
            if (account.accountType !== 'DEMAND_DEPOSIT' && account.accountType !== 'INSTALLMENT_SAVINGS') {
                return null;
            }
            const form = document.createElement('form');
            form.action = contextPath + '/codef-demo/transactions';
            form.method = 'post';
            [['accountId', account.accountId], ['transactionKind', account.accountType]].forEach(([name, value]) => {
                const input = document.createElement('input');
                input.type = 'hidden'; input.name = name; input.value = value; form.appendChild(input);
            });
            const button = document.createElement('button');
            button.className = 'card-button'; button.type = 'submit';
            button.textContent = account.accountType === 'INSTALLMENT_SAVINGS' ? '최근 납입내역 보기' : '최근 거래내역 보기';
            form.appendChild(button);
            return form;
        }

        try {
            const response = await fetch(
                contextPath + '/api/v1/accounts?userId=' + encodeURIComponent(userId) + '&refresh=' + refresh);
            if (!response.ok) throw new Error((await response.text()) || '계좌 목록을 불러오지 못했습니다.');
            const accounts = (await response.json()).filter(account =>
                account.institutionCode === organizationCode && account.businessType === businessType);
            loading.hidden = true;
            grid.hidden = false;
            if (accounts.length === 0) {
                grid.className = 'empty-card';
                grid.textContent = '조회된 연동 계좌가 없습니다.';
                return;
            }
            accounts.forEach(account => {
                const card = document.createElement('article');
                card.className = 'account-card';
                appendText(card, 'category', account.institutionName + ' · ' + account.accountType);
                const title = document.createElement('h2');
                title.textContent = account.productName || '연동 계좌';
                card.appendChild(title);
                appendText(card, 'account-number', account.accountMasked);
                appendText(card, 'balance', currency.format(account.currentBalance));
                if (account.maturityDate) appendText(card, 'form-note', '만기일: ' + account.maturityDate);
                const button = transactionButton(account);
                if (button) card.appendChild(button);
                grid.appendChild(card);
            });
        } catch (exception) {
            loading.hidden = true;
            error.hidden = false;
            error.textContent = exception.message;
        }
    }());
</script>
</body>
</html>
