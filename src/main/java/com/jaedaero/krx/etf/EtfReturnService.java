package com.jaedaero.krx.etf;

import com.jaedaero.krx.common.KrxApiException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class EtfReturnService {

    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final int MAX_PREVIOUS_TRADING_DAY_LOOKUP = 31;

    private final KrxEtfClient krxEtfClient;

    public EtfReturnService(KrxEtfClient krxEtfClient) {
        this.krxEtfClient = krxEtfClient;
    }

    public EtfReturnResponse getReturns(String isuCd, LocalDate requestedAsOfDate) {
        EtfDailyTradingInfo asOf = findLatestAtOrBefore(isuCd, requestedAsOfDate)
                .orElseThrow(() -> new KrxApiException("해당 종목의 기준일 ETF 데이터를 찾을 수 없습니다.", 404));
        LocalDate asOfDate = LocalDate.parse(asOf.basDd(), BASIC_DATE);
        BigDecimal asOfClosePrice = price(asOf.tddClsprc());

        List<EtfReturnPeriod> returns = List.of(
                calculate("SIX_MONTHS", asOfDate.minusMonths(6), isuCd, asOfClosePrice),
                calculate("YEAR_TO_DATE", asOfDate.withDayOfYear(1), isuCd, asOfClosePrice),
                calculate("ONE_YEAR", asOfDate.minusYears(1), isuCd, asOfClosePrice),
                calculate("TWO_YEARS", asOfDate.minusYears(2), isuCd, asOfClosePrice));
        return new EtfReturnResponse(
                asOf.isuCd(), asOf.isuNm(), format(requestedAsOfDate), asOf.basDd(), asOf.tddClsprc(), returns);
    }

    private EtfReturnPeriod calculate(String period, LocalDate requestedBaseDate, String isuCd, BigDecimal asOfClosePrice) {
        Optional<EtfDailyTradingInfo> base = findEarliestAtOrAfter(isuCd, requestedBaseDate);
        if (base.isEmpty()) {
            return new EtfReturnPeriod(period, format(requestedBaseDate), null, null, null, false);
        }
        EtfDailyTradingInfo baseInfo = base.get();
        BigDecimal baseClosePrice = price(baseInfo.tddClsprc());
        BigDecimal returnRate = asOfClosePrice.subtract(baseClosePrice)
                .multiply(BigDecimal.valueOf(100))
                .divide(baseClosePrice, 2, RoundingMode.HALF_UP);
        return new EtfReturnPeriod(
                period, format(requestedBaseDate), baseInfo.basDd(), baseInfo.tddClsprc(), returnRate, true);
    }

    private Optional<EtfDailyTradingInfo> findLatestAtOrBefore(String isuCd, LocalDate date) {
        for (int daysBack = 0; daysBack <= MAX_PREVIOUS_TRADING_DAY_LOOKUP; daysBack++) {
            String dateText = format(date.minusDays(daysBack));
            EtfDailyTradingResponse response = krxEtfClient.getDailyTrading(dateText);
            Optional<EtfDailyTradingInfo> found = response.outBlock1() == null
                    ? Optional.empty()
                    : response.outBlock1().stream()
                            .filter(item -> isuCd.equals(item.isuCd()) && isPrice(item.tddClsprc()))
                            .findFirst();
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    private Optional<EtfDailyTradingInfo> findEarliestAtOrAfter(String isuCd, LocalDate date) {
        for (int daysForward = 0; daysForward <= MAX_PREVIOUS_TRADING_DAY_LOOKUP; daysForward++) {
            String dateText = format(date.plusDays(daysForward));
            EtfDailyTradingResponse response = krxEtfClient.getDailyTrading(dateText);
            Optional<EtfDailyTradingInfo> found = response.outBlock1() == null
                    ? Optional.empty()
                    : response.outBlock1().stream()
                            .filter(item -> isuCd.equals(item.isuCd()) && isPrice(item.tddClsprc()))
                            .findFirst();
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    private BigDecimal price(String value) {
        try {
            return new BigDecimal(value.replace(",", ""));
        } catch (RuntimeException exception) {
            throw new KrxApiException("KRX ETF 종가 데이터 형식이 올바르지 않습니다.", 502, exception);
        }
    }

    private boolean isPrice(String value) {
        return value != null && !value.isBlank() && !"-".equals(value);
    }

    private String format(LocalDate date) {
        return date.format(BASIC_DATE);
    }
}
