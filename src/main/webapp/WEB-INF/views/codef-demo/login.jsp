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
        <p class="hero-description">입력한 인증정보는 CODEF 연결 요청에만 사용하며, 비밀번호는 저장하지 않습니다.</p>
    </section>

    <section class="form-card">
        <h2>금융기관 연결</h2>
        <c:if test="${not empty errorMessage}">
            <p class="alert error"><c:out value="${errorMessage}"/></p>
        </c:if>
        <form action="${pageContext.request.contextPath}/codef-demo/connect" method="post">
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
        <p class="form-note">ID/PW 로그인 방식만 지원합니다.</p>
    </section>
</main>
<script>
    (function () {
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
    }());
</script>
</body>
</html>
