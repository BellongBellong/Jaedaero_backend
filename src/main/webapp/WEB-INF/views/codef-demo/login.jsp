<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>제대로 · 금융 계좌 연결</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/codef-demo.css">
</head>
<body>
<main class="page-shell login-shell">
    <section class="hero-card">
        <p class="eyebrow">제대로 · CODEF DEMO</p>
        <h1>은행·증권 계좌를 연결하고<br>보유 자산을 확인하세요.</h1>
        <p class="hero-description">한 번 연결한 기관은 다음부터 비밀번호 입력 없이 계좌를 다시 불러올 수 있습니다.</p>
    </section>

    <c:if test="${not empty savedInstitutions}">
        <section class="form-card saved-institutions-card">
            <h2>저장된 기관</h2>
            <p class="form-note">CODEF Connected ID로 최신 계좌를 다시 조회합니다. 비밀번호는 화면에 표시하지 않습니다.</p>
            <div class="saved-institution-list">
                <c:forEach var="institution" items="${savedInstitutions}">
                    <form class="saved-institution" action="${pageContext.request.contextPath}/codef-demo/accounts" method="get">
                        <input type="hidden" name="organizationCode" value="${institution.organizationCode}">
                        <input type="hidden" name="businessType" value="${institution.businessType}">
                        <input type="hidden" name="refresh" value="true">
                        <div>
                            <strong><c:out value="${institution.institutionName}"/></strong>
                            <span><c:out value="${institution.loginIdDisplay}"/></span>
                        </div>
                        <button class="secondary-button" type="submit">계좌 불러오기</button>
                    </form>
                </c:forEach>
            </div>
        </section>
    </c:if>

    <section class="form-card">
        <h2>금융기관 연결</h2>
        <c:if test="${not empty errorMessage}">
            <p class="alert error"><c:out value="${errorMessage}"/></p>
        </c:if>
        <form id="accountConnectForm">
            <label for="businessType">기관 구분</label>
            <select id="businessType" name="businessType" required>
                <option value="BK">은행</option>
                <option value="ST">증권사</option>
            </select>

            <div id="bankInstitutionField">
            <label for="bankOrganizationCode">은행 선택</label>
            <select id="bankOrganizationCode" name="organizationCode" required>
                <option value="">은행을 선택해주세요</option>
                <c:forEach var="bank" items="${banks}">
                    <option value="${bank.organizationCode}"><c:out value="${bank.displayName}"/></option>
                </c:forEach>
            </select>
            </div>

            <div id="securitiesInstitutionField" hidden>
            <label for="securitiesOrganizationCode">증권사 선택</label>
            <select id="securitiesOrganizationCode" name="organizationCode" disabled>
                <option value="">증권사를 선택해주세요</option>
                <c:forEach var="securitiesInstitution" items="${securities}">
                    <option value="${securitiesInstitution.organizationCode}"><c:out value="${securitiesInstitution.displayName}"/></option>
                </c:forEach>
            </select>
            </div>

            <label for="loginId">인터넷뱅킹 ID</label>
            <input id="loginId" name="loginId" type="text" autocomplete="username" required>

            <label for="password">인터넷뱅킹 비밀번호</label>
            <input id="password" name="password" type="password" autocomplete="current-password" required>

            <label for="birthDate">생년월일 <span>(기관이 요구하는 경우만, YYMMDD)</span></label>
            <input id="birthDate" name="birthDate" type="text" inputmode="numeric" maxlength="6" placeholder="예: 990101">

            <button class="primary-button" type="submit">계좌 연결 및 조회</button>
        </form>
        <p class="form-note">ID/PW 로그인 방식만 지원합니다. 연결에 성공하면 로그인 정보는 암호화해 저장합니다.</p>
    </section>
</main>
<script>
    (function () {
        const contextPath = '${pageContext.request.contextPath}';
        const demoUserId = ${demoUserId};
        const connectForm = document.getElementById('accountConnectForm');
        const businessType = document.getElementById('businessType');
        const bankField = document.getElementById('bankInstitutionField');
        const securitiesField = document.getElementById('securitiesInstitutionField');
        const bankSelect = document.getElementById('bankOrganizationCode');
        const securitiesSelect = document.getElementById('securitiesOrganizationCode');

        function updateInstitutionField() {
            const securities = businessType.value === 'ST';
            bankField.hidden = securities;
            securitiesField.hidden = !securities;
            bankSelect.disabled = securities;
            bankSelect.required = !securities;
            securitiesSelect.disabled = !securities;
            securitiesSelect.required = securities;
        }

        businessType.addEventListener('change', updateInstitutionField);
        updateInstitutionField();

        connectForm.addEventListener('submit', async function (event) {
            event.preventDefault();
            const submitButton = connectForm.querySelector('button[type="submit"]');
            const errorMessage = document.querySelector('.alert.error');
            if (errorMessage) errorMessage.remove();
            submitButton.disabled = true;
            submitButton.textContent = '계좌 연결 중...';

            const organizationCode = businessType.value === 'ST'
                ? securitiesSelect.value
                : bankSelect.value;
            try {
                const response = await fetch(contextPath + '/api/v1/accounts/connect', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({
                        userId: demoUserId,
                        organizationCode: organizationCode,
                        businessType: businessType.value,
                        loginId: document.getElementById('loginId').value,
                        password: document.getElementById('password').value,
                        birthDate: document.getElementById('birthDate').value || null
                    })
                });
                if (!response.ok) {
                    throw new Error((await response.text()) || '계좌 연결에 실패했습니다.');
                }
                window.location.assign(
                    contextPath + '/codef-demo/accounts?organizationCode='
                    + encodeURIComponent(organizationCode)
                    + '&businessType=' + encodeURIComponent(businessType.value));
            } catch (error) {
                const alert = document.createElement('p');
                alert.className = 'alert error';
                alert.textContent = error.message;
                connectForm.parentNode.insertBefore(alert, connectForm);
                submitButton.disabled = false;
                submitButton.textContent = '계좌 연결 및 조회';
            }
        });
    }());
</script>
</body>
</html>
