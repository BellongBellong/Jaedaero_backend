package com.jaedaero.krx.etf;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(tags = "KRX ETF 시세")
@RequestMapping("/api/v1/etfs")
public class KrxEtfController {

    private final KrxEtfClient krxEtfClient;
    private final EtfReturnService etfReturnService;
    private final EtfMarketOverviewService etfMarketOverviewService;

    public KrxEtfController(
            KrxEtfClient krxEtfClient,
            EtfReturnService etfReturnService,
            EtfMarketOverviewService etfMarketOverviewService) {
        this.krxEtfClient = krxEtfClient;
        this.etfReturnService = etfReturnService;
        this.etfMarketOverviewService = etfMarketOverviewService;
    }

    @ApiOperation(
            value = "ETF 일별매매정보 조회",
            notes = "KRX 기준일자의 전체 ETF 매매정보를 조회합니다. 서버의 application-local.properties에 krx.api.auth-key 설정이 필요합니다.",
            response = EtfDailyTradingResponse.class)
    @ApiResponses({
        @ApiResponse(code = 200, message = "조회 성공", response = EtfDailyTradingResponse.class),
        @ApiResponse(code = 400, message = "기준일자 형식 또는 값이 올바르지 않음"),
        @ApiResponse(code = 502, message = "KRX API 요청 또는 응답 처리 실패"),
        @ApiResponse(code = 503, message = "KRX 인증키 미설정 또는 KRX 요청 중단")
    })
    @GetMapping("/daily-trading")
    public EtfDailyTradingResponse getDailyTrading(
            @ApiParam(value = "기준일자(yyyyMMdd)", required = true, example = "20260728")
                    @RequestParam String basDd) {
        validateDate(basDd);
        return krxEtfClient.getDailyTrading(basDd);
    }

    @ApiOperation(
            value = "ETF 기간별 수익률 조회",
            notes = "기준일이 휴장일이면 직전 거래일 종가를 사용합니다. 6개월·연초 이후·1년·2년의 시작일이 휴장일이면 다음 거래일 종가를 사용하며, 해당 기간에 ETF가 상장되지 않았다면 available=false로 반환합니다.",
            response = EtfReturnResponse.class)
    @ApiResponses({
        @ApiResponse(code = 200, message = "조회 성공", response = EtfReturnResponse.class),
        @ApiResponse(code = 400, message = "종목코드 또는 기준일 값이 올바르지 않음"),
        @ApiResponse(code = 404, message = "기준일 ETF 데이터를 찾을 수 없음"),
        @ApiResponse(code = 502, message = "KRX API 요청 또는 응답 처리 실패"),
        @ApiResponse(code = 503, message = "KRX 인증키 미설정 또는 KRX 요청 중단")
    })
    @GetMapping("/{isuCd}/returns")
    public EtfReturnResponse getReturns(
            @ApiParam(value = "ETF 종목코드", required = true, example = "069500")
                    @org.springframework.web.bind.annotation.PathVariable String isuCd,
            @ApiParam(value = "기준일(yyyyMMdd). 생략하면 오늘", example = "20260728")
                    @RequestParam(required = false) String asOfDate) {
        if (isuCd == null || !isuCd.matches("\\d{6}")) {
            throw new IllegalArgumentException("ETF 종목코드는 6자리 숫자여야 합니다.");
        }
        LocalDate requestedDate = asOfDate == null || asOfDate.isBlank() ? LocalDate.now() : parseDate(asOfDate);
        if (requestedDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("기준일은 오늘보다 늦을 수 없습니다.");
        }
        return etfReturnService.getReturns(isuCd, requestedDate);
    }

    @ApiOperation(
            value = "ETF 시세 및 수익률 요약 조회",
            notes = "기준일 전체 ETF의 시세와 6개월·연초 이후·1년·2년 수익률을 한 번에 조회합니다. 데모 첫 화면에서 사용합니다.",
            response = EtfMarketOverviewResponse.class)
    @ApiResponses({
        @ApiResponse(code = 200, message = "조회 성공", response = EtfMarketOverviewResponse.class),
        @ApiResponse(code = 400, message = "기준일 값이 올바르지 않음"),
        @ApiResponse(code = 404, message = "기준일 ETF 데이터를 찾을 수 없음"),
        @ApiResponse(code = 502, message = "KRX API 요청 또는 응답 처리 실패"),
        @ApiResponse(code = 503, message = "KRX 인증키 미설정 또는 KRX 요청 중단")
    })
    @GetMapping("/market-overview")
    public EtfMarketOverviewResponse getMarketOverview(
            @ApiParam(value = "기준일(yyyyMMdd). 생략하면 오늘", example = "20260729")
                    @RequestParam(required = false) String asOfDate) {
        LocalDate requestedDate = asOfDate == null || asOfDate.isBlank() ? LocalDate.now() : parseDate(asOfDate);
        if (requestedDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("기준일은 오늘보다 늦을 수 없습니다.");
        }
        return etfMarketOverviewService.getOverview(requestedDate);
    }

    private void validateDate(String basDd) {
        parseDate(basDd);
    }

    private LocalDate parseDate(String basDd) {
        if (basDd == null || !basDd.matches("\\d{8}")) {
            throw new IllegalArgumentException("기준일자는 yyyyMMdd 형식이어야 합니다.");
        }
        try {
            return LocalDate.parse(basDd, DateTimeFormatter.BASIC_ISO_DATE);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("기준일자가 올바르지 않습니다.");
        }
    }
}
