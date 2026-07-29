<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>제대로 · ETF 조회</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/resources/css/etf-demo.css">
</head>
<body>
<main class="etf-page">
    <header class="etf-hero">
        <p class="eyebrow">KRX ETF MARKET DATA</p>
        <h1>ETF 시세와 기간별 수익률</h1>
        <p>기준일의 ETF를 검색하고 6개월·연초 이후·1년·2년 수익률을 확인하세요.</p>
    </header>

    <section class="search-card" aria-label="ETF 검색">
        <label for="baseDate">기준일</label>
        <input id="baseDate" inputmode="numeric" maxlength="8" value="${defaultDate}" placeholder="yyyyMMdd">
        <label for="keyword">종목명 또는 종목코드</label>
        <input id="keyword" placeholder="예: KODEX 200 또는 069500">
        <button id="searchButton" type="button">ETF 조회</button>
    </section>

    <p id="message" class="message" role="status">기준일과 검색어를 입력한 뒤 조회하세요.</p>

    <section id="resultSection" class="result-card" hidden>
        <div class="section-heading">
            <div><p class="eyebrow">SEARCH RESULT</p><h2>ETF 목록</h2></div>
            <span id="resultCount"></span>
        </div>
        <div class="table-scroll">
            <table>
                <thead><tr><th>종목</th><th>종가</th><th>등락률</th><th>기간별 수익률</th><th>NAV</th><th>거래량</th><th></th></tr></thead>
                <tbody id="resultBody"></tbody>
            </table>
        </div>
    </section>

    <section id="returnSection" class="return-card" hidden>
        <div class="section-heading">
            <div><p class="eyebrow">RETURN SUMMARY</p><h2 id="returnTitle"></h2><p id="returnMeta" class="muted"></p></div>
        </div>
        <div id="returnGrid" class="return-grid"></div>
        <p class="notice">조회 기준일이 휴장일이면 직전 거래일, 투자 시작일이 휴장일이면 다음 거래일 종가를 사용합니다. 해당 ETF가 기준 기간에 아직 상장되지 않았다면 수익률을 표시하지 않습니다.</p>
    </section>
</main>
<script>
(() => {
    const contextPath = '${pageContext.request.contextPath}';
    const dateInput = document.getElementById('baseDate');
    const keywordInput = document.getElementById('keyword');
    const searchButton = document.getElementById('searchButton');
    const message = document.getElementById('message');
    const resultSection = document.getElementById('resultSection');
    const resultBody = document.getElementById('resultBody');
    const resultCount = document.getElementById('resultCount');
    const returnSection = document.getElementById('returnSection');
    const returnTitle = document.getElementById('returnTitle');
    const returnMeta = document.getElementById('returnMeta');
    const returnGrid = document.getElementById('returnGrid');
    const labels = {SIX_MONTHS: '6개월', YEAR_TO_DATE: '연초 이후', ONE_YEAR: '1년', TWO_YEARS: '2년'};
    const escapeHtml = value => String(value ?? '-').replace(/[&<>'"]/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[char]));
    const percent = value => value === null || value === undefined ? '-' : (Number(value) > 0 ? '+' : '') + Number(value).toFixed(2) + '%';
    const returnChips = returns => returns.map(item => '<span class="return-chip ' + (item.available ? (Number(item.returnRate) < 0 ? 'loss' : 'gain') : 'unavailable') + '">' + labels[item.period] + ' ' + (item.available ? percent(item.returnRate) : '-') + '</span>').join('');

    async function request(url) {
        const response = await fetch(url, {headers: {Accept: 'application/json'}});
        const body = await response.json().catch(() => ({}));
        if (!response.ok) throw new Error(body.message || '요청을 처리하지 못했습니다.');
        return body;
    }

    async function search() {
        const date = dateInput.value.trim();
        const keyword = keywordInput.value.trim().toLowerCase();
        if (!/^\d{8}$/.test(date)) { message.textContent = '기준일은 yyyyMMdd 형식으로 입력하세요.'; return; }
        searchButton.disabled = true;
        message.textContent = 'ETF 목록을 불러오는 중입니다…';
        returnSection.hidden = true;
        try {
            const data = await request(contextPath + '/api/v1/etfs/market-overview?asOfDate=' + encodeURIComponent(date));
            const items = (data.items || []).filter(item => !keyword || item.etf.ISU_CD.includes(keyword) || item.etf.ISU_NM.toLowerCase().includes(keyword));
            resultBody.innerHTML = items.map(item => {
                const etf = item.etf;
                return '<tr><td><strong>' + escapeHtml(etf.ISU_NM) + '</strong><small>' + escapeHtml(etf.ISU_CD) + '</small></td><td>' + escapeHtml(etf.TDD_CLSPRC) + '</td><td class="' + (String(etf.FLUC_RT).startsWith('-') ? 'negative' : 'positive') + '">' + escapeHtml(etf.FLUC_RT) + '%</td><td><div class="return-chips">' + returnChips(item.returns) + '</div></td><td>' + escapeHtml(etf.NAV) + '</td><td>' + escapeHtml(etf.ACC_TRDVOL) + '</td><td><button class="return-button" data-code="' + escapeHtml(etf.ISU_CD) + '">상세 보기</button></td></tr>';
            }).join('');
            resultSection.hidden = false;
            resultCount.textContent = items.length + '개';
            message.textContent = items.length ? '실제 적용 거래일: ' + data.asOfDate + ' · 각 ETF의 기간별 수익률을 바로 확인할 수 있습니다.' : '조건과 일치하는 ETF가 없습니다.';
        } catch (error) {
            resultSection.hidden = true;
            message.textContent = error.message;
        } finally { searchButton.disabled = false; }
    }

    async function showReturns(code) {
        message.textContent = '기간별 수익률을 계산하는 중입니다…';
        try {
            const data = await request(contextPath + '/api/v1/etfs/' + encodeURIComponent(code) + '/returns?asOfDate=' + encodeURIComponent(dateInput.value.trim()));
            returnTitle.textContent = data.isuNm + ' (' + data.isuCd + ')';
            returnMeta.textContent = '기준일 ' + data.asOfDate + ' · 종가 ' + data.closePrice;
            returnGrid.innerHTML = data.returns.map(item => '<article class="return-item ' + (item.available ? (Number(item.returnRate) < 0 ? 'loss' : 'gain') : 'unavailable') + '"><span>' + labels[item.period] + '</span><strong>' + (item.available ? percent(item.returnRate) : '데이터 없음') + '</strong><small>' + (item.available ? item.baseDate + ' 종가 ' + item.baseClosePrice : '상장 전 또는 기준 데이터 없음') + '</small></article>').join('');
            returnSection.hidden = false;
            returnSection.scrollIntoView({behavior: 'smooth', block: 'start'});
            message.textContent = '수익률 계산을 완료했습니다.';
        } catch (error) { message.textContent = error.message; }
    }

    searchButton.addEventListener('click', search);
    keywordInput.addEventListener('keydown', event => { if (event.key === 'Enter') search(); });
    resultBody.addEventListener('click', event => { const button = event.target.closest('.return-button'); if (button) showReturns(button.dataset.code); });
    search();
})();
</script>
</body>
</html>
