<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>제대로 · 은행 계좌 연결</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/codef-demo.css">
</head>
<body>
<main class="page-shell login-shell">
    <section class="hero-card">
        <p class="eyebrow">제대로 · CODEF DEMO</p>
        <h1>은행 계좌를 연결하고<br>최근 소비를 확인하세요.</h1>
        <p class="hero-description">입력한 인증정보는 CODEF 연결 요청에만 사용하며, 이 데모는 비밀번호와 Connected ID를 DB에 저장하지 않습니다.</p>
    </section>

    <section class="form-card">
        <h2>은행 연결</h2>
        <c:if test="${not empty errorMessage}">
            <p class="alert error"><c:out value="${errorMessage}"/></p>
        </c:if>
        <form action="${pageContext.request.contextPath}/codef-demo/connect" method="post">
            <label for="organizationCode">은행 선택</label>
            <select id="organizationCode" name="organizationCode" required>
                <option value="">은행을 선택해주세요</option>
                <c:forEach var="bank" items="${banks}">
                    <option value="${bank.organizationCode}"><c:out value="${bank.displayName}"/></option>
                </c:forEach>
            </select>

            <label for="loginId">인터넷뱅킹 ID</label>
            <input id="loginId" name="loginId" type="text" autocomplete="username" required>

            <label for="password">인터넷뱅킹 비밀번호</label>
            <input id="password" name="password" type="password" autocomplete="current-password" required>

            <label for="birthday">생년월일 <span>(필요한 은행만, YYMMDD)</span></label>
            <input id="birthday" name="birthday" type="text" inputmode="numeric" maxlength="6" placeholder="예: 990101">

            <button class="primary-button" type="submit">계좌 연결 및 조회</button>
        </form>
        <p class="form-note">현재는 ID/PW 로그인 방식 데모입니다. 공동인증서 방식은 별도 인증서 전송 화면이 필요합니다.</p>
    </section>
</main>
</body>
</html>
