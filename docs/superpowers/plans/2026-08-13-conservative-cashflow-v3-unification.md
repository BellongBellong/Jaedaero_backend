# Conservative Cashflow V3 Unification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the dual `expectedAsset`/`potentialExpectedAsset` final-value split in the cashflow and simulation (What-if) domains with a single "전역 예상 자산" total, and make the underlying interest/investment-return math match how the linked financial products actually work (per-installment simple interest for 장병내일준비적금, CODEF-sourced cost basis for investment principal, salary-proportional scaling for applied spending/investment amounts).

**Architecture:** `ConservativeMonthlyCashflowEngine` (shared by both domains) gets a rewritten `calculateProjectedBenefit` that folds in existing account balances (soldier saving, brokerage) alongside future monthly contributions, using per-installment date math instead of a flat monthly-compound approximation for savings. A new `AggregateInvestmentPrincipalProvider` (CODEF real / local mock / env-aware, mirroring the existing `BrokeragePositionProvider` pattern in `investmentguidance`) supplies the existing brokerage principal without requiring an active recurring investment plan. `CashflowCalculator` and `SimulationCalculator` both gain salary-ratio scaling for previously-flat applied spending/investment amounts, and both drop the `potentialExpectedAsset`/`returnsIncludedInExpectedAsset` fields in favor of a single `expectedAsset`.

**Tech Stack:** Spring Boot, MyBatis (XML mappers), JUnit 5, plain hand-rolled test doubles (no Mockito in this codebase's service tests — see `CashflowServiceImplTest`'s `InMemoryCashflowMapper` pattern).

**Spec:** [[로직_캐시플로우엔진]] §5-1 (`jaedaero_wiki/기능/로직_캐시플로우엔진.md`), cross-referenced from [[AI전략_UI_API_정합성_20260806]] Issue 20 and [[ERD_v2]]'s `cashflow_forecast` drift note. The spec lives in the project wiki (user's explicit location override), not `docs/superpowers/specs/`.

## Global Constraints

- 군적금 이율은 연 5% 고정 정책 상수, 정부 매칭률은 100% 고정 정책 상수 — `soldier_saving.interest_rate`(계좌별 실제 금리)는 계산에 쓰지 않는다.
- `expected_return_rate`(투자 목표수익률)는 **연 환산 값** 의미를 유지한다 — 재해석하거나 새 필드로 쪼개지 않는다.
- 비율-연동 소비/투자 스케일링은 서버 내부 계산에서만 쓰고, 요청/응답/DB 어디에도 비율을 저장하지 않는다 (금액이 항상 진실의 원천).
- local mock 환경(`app.environment=local`)에서는 증권 매입원가 데이터가 없으므로 "투자 원금 = 평가금액"으로 계속 근사한다 — 이 경로는 바꾸지 않는다.
- 원금 이중 합산 금지 원칙(`asset(m) = asset(m-1) + salary(m) - spending(m)`, 저축/투자는 순자산에 다시 더하지 않음)은 그대로 유지한다.

---

## File Structure

| File | Responsibility |
|---|---|
| `sql/migration/20260813_conservative_cashflow_v3.sql` | New migration: drop `potential_expected_asset` from both tables, add `soldier_saving_principal`/`investment_principal` to `cashflow_forecast` |
| `src/main/java/com/jaedaero/domain/cashflow/service/ConservativeMonthlyCashflowEngine.java` | Rewritten benefit calculation (existing balance + future installments) |
| `src/main/java/com/jaedaero/domain/cashflow/service/SoldierSavingInput.java` | Add `startDate` |
| `src/main/java/com/jaedaero/domain/cashflow/vo/SoldierSavingInputSourceVo.java` | Add `startDate` |
| `src/main/resources/mapper/cashflow/CashflowMapper.xml` | Select `saving.start_date`, relax NOT-NULL filters, add new columns to forecast insert/select |
| `src/main/java/com/jaedaero/domain/cashflow/service/DbCashflowInputProvider.java` | Wire `startDate` through |
| `src/main/java/com/jaedaero/domain/cashflow/dto/CashflowCalculationInputResponse.java` | Carry `soldierSavings` across the cashflow→simulation boundary |
| `src/main/java/com/jaedaero/domain/simulation/service/SimulationInput.java` | Add `soldierSavings` |
| `src/main/java/com/jaedaero/domain/simulation/service/CashflowSimulationInputProvider.java` | Wire `soldierSavings` through |
| `src/main/java/com/jaedaero/domain/cashflow/service/AggregateInvestmentPrincipalProvider.java` (new) | Interface: resolve linked brokerage principal without requiring a recurring plan |
| `src/main/java/com/jaedaero/domain/cashflow/service/CodefAggregateInvestmentPrincipalProvider.java` (new) | Sums CODEF `purchaseAmount` across all linked `ST` accounts |
| `src/main/java/com/jaedaero/domain/cashflow/service/LocalAggregateInvestmentPrincipalProvider.java` (new) | Sums `current_balance` across all linked `ST` accounts (local mock fallback) |
| `src/main/java/com/jaedaero/domain/cashflow/service/EnvironmentAwareAggregateInvestmentPrincipalProvider.java` (new) | Environment switch, mirrors `EnvironmentAwareBrokeragePositionProvider` |
| `src/main/java/com/jaedaero/domain/cashflow/service/CashflowCalculator.java` | Ratio-scaling for applied-strategy path, wire new engine + provider + backfill, single `expectedAsset` |
| `src/main/java/com/jaedaero/domain/cashflow/service/CashflowForecastCalculation.java` | Drop `potentialExpectedAsset`, add `soldierSavingPrincipal`/`investmentPrincipal` |
| `src/main/java/com/jaedaero/domain/cashflow/vo/CashflowForecastVo.java` | Same field changes |
| `src/main/java/com/jaedaero/domain/cashflow/dto/CashflowForecastResponse.java` | Same field changes |
| `src/main/java/com/jaedaero/domain/cashflow/service/impl/CashflowServiceImpl.java` | Wire new fields |
| `src/main/java/com/jaedaero/domain/simulation/service/SimulationCalculator.java` | Ratio-scaling (always-on), wire new engine + provider + backfill |
| `src/main/java/com/jaedaero/domain/simulation/dto/SimulationExpectedEffectResponse.java` | Drop `conservativeExpectedAsset`/`potentialExpectedAsset`/`returnsIncludedInExpectedAsset` |
| `src/main/java/com/jaedaero/domain/simulation/vo/SimulationVo.java`, `SimulationCalculationResult.java` | Drop `potentialExpectedAsset` |
| `src/main/resources/mapper/simulation/SimulationMapper.xml` | Drop `potential_expected_asset` column references |
| `src/test/java/com/jaedaero/domain/cashflow/service/ConservativeMonthlyCashflowEngineTest.java` | New tests for the rewritten benefit calc |
| `src/test/java/com/jaedaero/domain/cashflow/service/CashflowCalculatorTest.java` | Update for ratio-scaling + single asset |
| `src/test/java/com/jaedaero/domain/cashflow/service/CashflowServiceImplTest.java` | Update fixture/assertions |
| `src/test/java/com/jaedaero/domain/simulation/service/SimulationServiceImplTest.java` | Update fixture/assertions |

---

### Task 1: Thread `soldier_saving.start_date` through the input pipeline

**Files:**
- Modify: `src/main/java/com/jaedaero/domain/cashflow/service/SoldierSavingInput.java`
- Modify: `src/main/java/com/jaedaero/domain/cashflow/vo/SoldierSavingInputSourceVo.java`
- Modify: `src/main/resources/mapper/cashflow/CashflowMapper.xml:54-71`
- Modify: `src/main/java/com/jaedaero/domain/cashflow/service/DbCashflowInputProvider.java:36-45`
- Test: `src/test/java/com/jaedaero/domain/cashflow/service/DbCashflowInputProviderTest.java` (new)

**Interfaces:**
- Produces: `SoldierSavingInput(long currentBalance, long monthlyAmount, BigDecimal annualInterestRate, long governmentSupportExpected, LocalDate startDate, LocalDate maturityDate)` — `startDate` is new, inserted before the existing `maturityDate` (renamed from the old positional `endDate` argument — the record's accessor was already `maturityDate()`, only the constructor call site used `getEndDate()`).

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/jaedaero/domain/cashflow/service/DbCashflowInputProviderTest.java`:

```java
package com.jaedaero.domain.cashflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jaedaero.domain.auth.common.enums.SoldierType;
import com.jaedaero.domain.cashflow.mapper.CashflowMapper;
import com.jaedaero.domain.cashflow.vo.CashflowInputSourceVo;
import com.jaedaero.domain.cashflow.vo.SoldierSavingInputSourceVo;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class DbCashflowInputProviderTest {

  @Test
  void loadCarriesSoldierSavingStartDateIntoTheDomainRecord() {
    CashflowInputSourceVo source = new CashflowInputSourceVo();
    source.setBaseAsset(1_000_000L);
    source.setTargetAmount(30_000_000L);
    source.setMonthlySpendingAverage(500_000L);
    source.setSoldierType("ARMY");
    source.setEnlistmentDate(LocalDate.of(2026, 1, 1));
    source.setDischargeDate(LocalDate.of(2027, 6, 30));

    SoldierSavingInputSourceVo saving = new SoldierSavingInputSourceVo();
    saving.setCurrentBalance(2_000_000L);
    saving.setMonthlyAmount(400_000L);
    saving.setInterestRate(new java.math.BigDecimal("5.00"));
    saving.setGovernmentSupportExpected(0L);
    saving.setStartDate(LocalDate.of(2026, 1, 10));
    saving.setEndDate(LocalDate.of(2027, 6, 30));

    CashflowMapper mapper = new StubCashflowMapper(source, List.of(saving));
    CashflowInputProvider provider = new DbCashflowInputProvider(mapper);

    CashflowInput input = provider.load(1L);

    assertEquals(1, input.soldierSavings().size());
    assertEquals(LocalDate.of(2026, 1, 10), input.soldierSavings().get(0).startDate());
    assertEquals(LocalDate.of(2027, 6, 30), input.soldierSavings().get(0).maturityDate());
  }
}
```

You'll need a minimal `StubCashflowMapper` in the same test file (package-private, implements the parts of `CashflowMapper` this provider calls). Check `src/main/java/com/jaedaero/domain/cashflow/mapper/CashflowMapper.java` for the full interface — implement `findInputByUserId` and `findSoldierSavingsByUserId` to return the fixtures above, and throw `UnsupportedOperationException` for every other method.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.jaedaero.domain.cashflow.service.DbCashflowInputProviderTest"`
Expected: FAIL — compile error, `SoldierSavingInputSourceVo` has no `setStartDate`, `SoldierSavingInput` has no `startDate()`.

- [ ] **Step 3: Add `startDate` to the VO and record**

In `SoldierSavingInputSourceVo.java`, add a field:

```java
  private LocalDate startDate;
```//
(add it above the existing `private LocalDate endDate;` line — Lombok `@Getter`/`@Setter` on the class already generates `getStartDate()`/`setStartDate()`)

In `SoldierSavingInput.java`, change the record declaration:

```java
public record SoldierSavingInput(
    long currentBalance,
    long monthlyAmount,
    BigDecimal annualInterestRate,
    long governmentSupportExpected,
    LocalDate startDate,
    LocalDate maturityDate) {}
```

- [ ] **Step 4: Wire the mapper query and provider**

In `CashflowMapper.xml`, change the `findSoldierSavingsByUserId` query (lines 54-71):

```xml
    <select id="findSoldierSavingsByUserId"
            resultType="com.jaedaero.domain.cashflow.vo.SoldierSavingInputSourceVo">
        SELECT
            account.current_balance,
            saving.monthly_amount,
            saving.interest_rate,
            saving.government_support_expected,
            saving.start_date,
            saving.end_date
        FROM soldier_saving saving
        INNER JOIN connected_account account ON account.account_id = saving.account_id
        INNER JOIN codef_connection connection ON connection.connection_id = account.connection_id
        WHERE saving.user_id = #{userId}
          AND connection.status = 'ACTIVE'
          AND account.status = 'ACTIVE'
          AND saving.start_date IS NOT NULL
    </select>
```

(dropped the `monthly_amount IS NOT NULL`, `interest_rate IS NOT NULL`, `end_date IS NOT NULL` filters — the new calculation no longer needs those fields to be present; `start_date IS NOT NULL` is the only hard requirement since it drives the elapsed-months interest calc)

In `DbCashflowInputProvider.java`, change the mapping (lines 36-45):

```java
          cashflowMapper.findSoldierSavingsByUserId(userId).stream()
              .map(
                  saving ->
                      new SoldierSavingInput(
                          defaultIfNull(saving.getCurrentBalance()),
                          defaultIfNull(saving.getMonthlyAmount()),
                          saving.getInterestRate(),
                          defaultIfNull(saving.getGovernmentSupportExpected()),
                          saving.getStartDate(),
                          saving.getEndDate()))
              .toList(),
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew test --tests "com.jaedaero.domain.cashflow.service.DbCashflowInputProviderTest"`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/jaedaero/domain/cashflow/service/SoldierSavingInput.java \
        src/main/java/com/jaedaero/domain/cashflow/vo/SoldierSavingInputSourceVo.java \
        src/main/resources/mapper/cashflow/CashflowMapper.xml \
        src/main/java/com/jaedaero/domain/cashflow/service/DbCashflowInputProvider.java \
        src/test/java/com/jaedaero/domain/cashflow/service/DbCashflowInputProviderTest.java
git commit -m "feat: thread soldier_saving.start_date through cashflow input"
```

---

### Task 2: Rewrite `ConservativeMonthlyCashflowEngine.calculateProjectedBenefit`

**Files:**
- Modify: `src/main/java/com/jaedaero/domain/cashflow/service/ConservativeMonthlyCashflowEngine.java`
- Test: `src/test/java/com/jaedaero/domain/cashflow/service/ConservativeMonthlyCashflowEngineTest.java`

**Interfaces:**
- Consumes: `SoldierSavingInput` (from Task 1, now has `startDate()`/`maturityDate()`)
- Produces: new method signature —
  ```java
  ProjectedBenefit calculateProjectedBenefit(
      List<SoldierSavingInput> existingSoldierSavings,
      LocalDate calculationDate,
      List<Long> monthlySavingContributions,
      List<LocalDate> monthlySavingContributionDates,
      LocalDate dischargeDate,
      long existingInvestmentPrincipal,
      List<Long> monthlyInvestmentContributions,
      BigDecimal investmentAnnualReturnRate)
  ```
  `ProjectedBenefit`'s field shape is unchanged (`soldierSavingPrincipal`, `soldierSavingInterest`, `governmentMatchingSupport`, `investmentPrincipal`, `expectedInvestmentReturn`, `projectedBenefitAmount`) — only what feeds into it changes. This is a breaking signature change; Tasks 6 and 8 update the two call sites.

- [ ] **Step 1: Write the failing tests**

Add to `ConservativeMonthlyCashflowEngineTest.java` (keep the two existing `project(...)` tests as-is):

```java
  @Test
  void existingSoldierSavingBalanceEarnsRoughSimpleInterestByElapsedMonths() {
    ConservativeMonthlyCashflowEngine.ProjectedBenefit benefit =
        engine.calculateProjectedBenefit(
            List.of(
                new SoldierSavingInput(
                    12_000_000L, 0L, null, 0L,
                    LocalDate.of(2025, 1, 10),
                    LocalDate.of(2027, 1, 10))),
            LocalDate.of(2026, 1, 10),
            List.of(),
            List.of(),
            LocalDate.of(2027, 1, 10),
            0L,
            List.of(),
            java.math.BigDecimal.ZERO);

    // 12,000,000 * 5% * 12/12 = 600,000
    assertEquals(600_000L, benefit.soldierSavingInterest());
    assertEquals(12_000_000L, benefit.soldierSavingPrincipal());
    assertEquals(12_000_000L, benefit.governmentMatchingSupport());
  }

  @Test
  void futureSavingInstallmentsEarnInterestProratedByRemainingMonthsToMaturity() {
    ConservativeMonthlyCashflowEngine.ProjectedBenefit benefit =
        engine.calculateProjectedBenefit(
            List.of(),
            LocalDate.of(2026, 1, 10),
            List.of(500_000L, 500_000L),
            List.of(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 7, 10)),
            LocalDate.of(2027, 1, 10),
            0L,
            List.of(),
            java.math.BigDecimal.ZERO);

    // 1st installment: 12 months left -> 500,000 * 5% * 12/12 = 25,000
    // 2nd installment: 6 months left  -> 500,000 * 5% * 6/12  = 12,500
    assertEquals(37_500L, benefit.soldierSavingInterest());
    assertEquals(1_000_000L, benefit.soldierSavingPrincipal());
    assertEquals(1_000_000L, benefit.governmentMatchingSupport());
  }

  @Test
  void savingMaturityFallsBackToDischargeDateWhenAccountHasNoEndDate() {
    ConservativeMonthlyCashflowEngine.ProjectedBenefit benefit =
        engine.calculateProjectedBenefit(
            List.of(new SoldierSavingInput(0L, 0L, null, 0L, LocalDate.of(2026, 1, 10), null)),
            LocalDate.of(2026, 1, 10),
            List.of(1_000_000L),
            List.of(LocalDate.of(2026, 1, 10)),
            LocalDate.of(2026, 7, 10),
            0L,
            List.of(),
            java.math.BigDecimal.ZERO);

    // no end_date on the account -> falls back to dischargeDate (6 months out)
    // 1,000,000 * 5% * 6/12 = 25,000
    assertEquals(25_000L, benefit.soldierSavingInterest());
  }

  @Test
  void existingInvestmentPrincipalCompoundsAlongsideFutureContributions() {
    ConservativeMonthlyCashflowEngine.ProjectedBenefit benefit =
        engine.calculateProjectedBenefit(
            List.of(),
            LocalDate.of(2026, 1, 10),
            List.of(),
            List.of(),
            LocalDate.of(2026, 1, 10),
            1_000_000L,
            List.of(0L, 0L, 0L),
            new java.math.BigDecimal("12.00"));

    // 1,000,000 compounding at 1%/month for 3 months: 1,000,000 * 1.01^3 - 1,000,000 = 30,301
    assertEquals(30_301L, benefit.expectedInvestmentReturn());
    assertEquals(1_000_000L, benefit.investmentPrincipal());
  }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "com.jaedaero.domain.cashflow.service.ConservativeMonthlyCashflowEngineTest"`
Expected: FAIL — compile error (old 3-arg `calculateProjectedBenefit` signature doesn't accept these arguments).

- [ ] **Step 3: Replace `calculateProjectedBenefit` and `compoundReturn`**

In `ConservativeMonthlyCashflowEngine.java`, replace the whole `calculateProjectedBenefit` method and the `compoundReturn` method (current lines 50-71 and 81-100) with:

```java
  public ProjectedBenefit calculateProjectedBenefit(
      List<SoldierSavingInput> existingSoldierSavings,
      LocalDate calculationDate,
      List<Long> monthlySavingContributions,
      List<LocalDate> monthlySavingContributionDates,
      LocalDate dischargeDate,
      long existingInvestmentPrincipal,
      List<Long> monthlyInvestmentContributions,
      BigDecimal investmentAnnualReturnRate) {
    LocalDate savingMaturityDate = resolveSavingMaturityDate(existingSoldierSavings, dischargeDate);

    long existingSavingBalance = 0L;
    long existingSavingInterest = 0L;
    for (SoldierSavingInput saving : existingSoldierSavings) {
      existingSavingBalance = Math.addExact(existingSavingBalance, saving.currentBalance());
      long elapsedMonths =
          Math.max(0L, ChronoUnit.MONTHS.between(saving.startDate(), calculationDate));
      existingSavingInterest =
          Math.addExact(
              existingSavingInterest,
              simpleInterest(
                  saving.currentBalance(), SOLDIER_SAVING_ANNUAL_INTEREST_RATE, elapsedMonths));
    }

    long futureSavingInterest = 0L;
    for (int index = 0; index < monthlySavingContributions.size(); index++) {
      long amount = monthlySavingContributions.get(index);
      long remainingMonths =
          Math.max(
              0L,
              ChronoUnit.MONTHS.between(
                  monthlySavingContributionDates.get(index), savingMaturityDate));
      futureSavingInterest =
          Math.addExact(
              futureSavingInterest,
              simpleInterest(amount, SOLDIER_SAVING_ANNUAL_INTEREST_RATE, remainingMonths));
    }

    long savingPrincipal = Math.addExact(existingSavingBalance, sum(monthlySavingContributions));
    long savingInterest = Math.addExact(existingSavingInterest, futureSavingInterest);
    long governmentMatchingSupport = rateAmount(savingPrincipal, GOVERNMENT_MATCHING_RATE);

    long investmentPrincipal =
        Math.addExact(existingInvestmentPrincipal, sum(monthlyInvestmentContributions));
    long investmentReturn =
        compoundReturn(
            existingInvestmentPrincipal, monthlyInvestmentContributions, investmentAnnualReturnRate);

    long projectedBenefitAmount =
        Math.addExact(Math.addExact(savingInterest, governmentMatchingSupport), investmentReturn);
    return new ProjectedBenefit(
        savingPrincipal,
        savingInterest,
        governmentMatchingSupport,
        investmentPrincipal,
        investmentReturn,
        projectedBenefitAmount);
  }

  private LocalDate resolveSavingMaturityDate(
      List<SoldierSavingInput> existingSoldierSavings, LocalDate dischargeDate) {
    return existingSoldierSavings.stream()
        .map(SoldierSavingInput::maturityDate)
        .filter(java.util.Objects::nonNull)
        .findFirst()
        .orElse(dischargeDate);
  }

  private long simpleInterest(long principal, BigDecimal annualRatePercent, long months) {
    if (principal == 0L || months <= 0L) {
      return 0L;
    }
    return BigDecimal.valueOf(principal)
        .multiply(annualRatePercent)
        .multiply(BigDecimal.valueOf(months))
        .divide(PERCENT.multiply(MONTHS_PER_YEAR), 0, RoundingMode.HALF_UP)
        .longValueExact();
  }

  private long sum(List<Long> contributions) {
    long total = 0L;
    for (Long contribution : contributions) {
      total = Math.addExact(total, contribution == null ? 0L : contribution);
    }
    return total;
  }

  private long compoundReturn(
      long existingPrincipal, List<Long> contributions, BigDecimal annualRatePercent) {
    if (annualRatePercent == null || annualRatePercent.signum() == 0) {
      return 0L;
    }
    BigDecimal monthlyRate =
        annualRatePercent
            .divide(PERCENT, RETURN_MATH_CONTEXT)
            .divide(MONTHS_PER_YEAR, RETURN_MATH_CONTEXT);
    BigDecimal growthFactor = BigDecimal.ONE.add(monthlyRate);
    BigDecimal balance = BigDecimal.valueOf(existingPrincipal);
    for (Long contribution : contributions) {
      balance =
          balance
              .multiply(growthFactor, RETURN_MATH_CONTEXT)
              .add(BigDecimal.valueOf(contribution == null ? 0L : contribution));
    }
    long totalPrincipal = Math.addExact(existingPrincipal, sum(contributions));
    return balance
        .subtract(BigDecimal.valueOf(totalPrincipal))
        .setScale(0, RoundingMode.HALF_UP)
        .longValueExact();
  }
```

Remove the now-unused `rateAmount` duplication check — it already exists further down the file (keep it, both new and old code share it). Add the missing import at the top of the file:

```java
import java.time.temporal.ChronoUnit;
```

Also bump the policy version constant (same file, near the top):

```java
  public static final String CALCULATION_POLICY_VERSION = "CONSERVATIVE_CASHFLOW_V3_20260813";
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests "com.jaedaero.domain.cashflow.service.ConservativeMonthlyCashflowEngineTest"`
Expected: PASS (all 6 tests: the 2 pre-existing `project` tests plus the 4 new ones)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/jaedaero/domain/cashflow/service/ConservativeMonthlyCashflowEngine.java \
        src/test/java/com/jaedaero/domain/cashflow/service/ConservativeMonthlyCashflowEngineTest.java
git commit -m "feat: prorate soldier saving interest per installment, seed investment compounding with existing principal"
```

---

### Task 3: `AggregateInvestmentPrincipalProvider` (linked brokerage principal, no plan required)

**Files:**
- Create: `src/main/java/com/jaedaero/domain/cashflow/service/AggregateInvestmentPrincipalProvider.java`
- Create: `src/main/java/com/jaedaero/domain/cashflow/service/CodefAggregateInvestmentPrincipalProvider.java`
- Create: `src/main/java/com/jaedaero/domain/cashflow/service/LocalAggregateInvestmentPrincipalProvider.java`
- Create: `src/main/java/com/jaedaero/domain/cashflow/service/EnvironmentAwareAggregateInvestmentPrincipalProvider.java`
- Test: `src/test/java/com/jaedaero/domain/cashflow/service/EnvironmentAwareAggregateInvestmentPrincipalProviderTest.java`

**Interfaces:**
- Produces: `interface AggregateInvestmentPrincipalProvider { Optional<Long> resolveLinkedPrincipal(long userId); }` — empty `Optional` means "no linked securities account", which Tasks 6/8 interpret as "use the salary-based backfill instead". A present value (possibly `0L`, e.g. a linked account with no holdings yet) means "use this, no backfill".

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/jaedaero/domain/cashflow/service/EnvironmentAwareAggregateInvestmentPrincipalProviderTest.java`:

```java
package com.jaedaero.domain.cashflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class EnvironmentAwareAggregateInvestmentPrincipalProviderTest {

  @Test
  void delegatesToLocalProviderWhenAppEnvironmentIsLocal() {
    AggregateInvestmentPrincipalProvider provider =
        new EnvironmentAwareAggregateInvestmentPrincipalProvider(
            userId -> Optional.of(500_000L), userId -> Optional.of(999_999L), "local");

    assertEquals(Optional.of(500_000L), provider.resolveLinkedPrincipal(1L));
  }

  @Test
  void delegatesToCodefProviderInNonLocalEnvironments() {
    AggregateInvestmentPrincipalProvider provider =
        new EnvironmentAwareAggregateInvestmentPrincipalProvider(
            userId -> Optional.of(500_000L), userId -> Optional.of(999_999L), "production");

    assertEquals(Optional.of(999_999L), provider.resolveLinkedPrincipal(1L));
  }

  @Test
  void emptyOptionalMeansNoLinkedAccount() {
    AggregateInvestmentPrincipalProvider provider =
        new EnvironmentAwareAggregateInvestmentPrincipalProvider(
            userId -> Optional.empty(), userId -> Optional.empty(), "production");

    assertTrue(provider.resolveLinkedPrincipal(1L).isEmpty());
  }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.jaedaero.domain.cashflow.service.EnvironmentAwareAggregateInvestmentPrincipalProviderTest"`
Expected: FAIL — `AggregateInvestmentPrincipalProvider` and `EnvironmentAwareAggregateInvestmentPrincipalProvider` don't exist yet.

- [ ] **Step 3: Write the interface**

`AggregateInvestmentPrincipalProvider.java`:

```java
package com.jaedaero.domain.cashflow.service;

import java.util.Optional;

/**
 * 특정 종목 매칭 없이 사용자의 연동 증권계좌 전체 매입원금을 합산합니다.
 *
 * <p>빈 Optional은 연동된 증권계좌가 없다는 뜻이며, 호출자는 계급별 월급 기반 백필로 대체한다.
 */
public interface AggregateInvestmentPrincipalProvider {

  Optional<Long> resolveLinkedPrincipal(long userId);
}
```

- [ ] **Step 4: Write the CODEF-backed implementation**

`CodefAggregateInvestmentPrincipalProvider.java`:

```java
package com.jaedaero.domain.cashflow.service;

import com.jaedaero.domain.codef.account.CodefSecuritiesInquiryService;
import com.jaedaero.domain.codef.account.SecuritiesHoldingResponse;
import com.jaedaero.domain.codef.persistence.CodefPersistenceRepository;
import com.jaedaero.domain.codef.persistence.StoredConnectedAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class CodefAggregateInvestmentPrincipalProvider implements AggregateInvestmentPrincipalProvider {

  private static final String SECURITIES_BUSINESS_TYPE = "ST";

  private final CodefPersistenceRepository repository;
  private final CodefSecuritiesInquiryService securitiesInquiryService;

  public CodefAggregateInvestmentPrincipalProvider(
      CodefPersistenceRepository repository, CodefSecuritiesInquiryService securitiesInquiryService) {
    this.repository = repository;
    this.securitiesInquiryService = securitiesInquiryService;
  }

  @Override
  public Optional<Long> resolveLinkedPrincipal(long userId) {
    List<StoredConnectedAccount> securitiesAccounts =
        repository.findAccountsByUserId(userId).stream()
            .filter(account -> SECURITIES_BUSINESS_TYPE.equals(account.businessType()))
            .toList();
    if (securitiesAccounts.isEmpty()) {
      return Optional.empty();
    }
    long principal = 0L;
    for (StoredConnectedAccount account : securitiesAccounts) {
      List<SecuritiesHoldingResponse> holdings =
          securitiesInquiryService.getFinancialAssets(userId, account.accountId()).holdings();
      principal =
          Math.addExact(
              principal, holdings.stream().mapToLong(SecuritiesHoldingResponse::purchaseAmount).sum());
    }
    return Optional.of(principal);
  }
}
```

(check `StoredConnectedAccount`'s record field name for the account id — `src/main/java/com/jaedaero/domain/codef/persistence/StoredConnectedAccount.java` — adjust `account.accountId()` to whatever accessor it actually exposes if different)

- [ ] **Step 5: Write the local mock implementation**

`LocalAggregateInvestmentPrincipalProvider.java`:

```java
package com.jaedaero.domain.cashflow.service;

import com.jaedaero.domain.codef.persistence.CodefPersistenceRepository;
import com.jaedaero.domain.codef.persistence.StoredConnectedAccount;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** local mock 환경에서는 매입원가가 없으므로 평가금액(잔액)을 원금으로 근사한다. */
@Component
public class LocalAggregateInvestmentPrincipalProvider implements AggregateInvestmentPrincipalProvider {

  private static final String SECURITIES_BUSINESS_TYPE = "ST";

  private final CodefPersistenceRepository repository;

  public LocalAggregateInvestmentPrincipalProvider(CodefPersistenceRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<Long> resolveLinkedPrincipal(long userId) {
    var securitiesAccounts =
        repository.findAccountsByUserId(userId).stream()
            .filter(account -> SECURITIES_BUSINESS_TYPE.equals(account.businessType()))
            .toList();
    if (securitiesAccounts.isEmpty()) {
      return Optional.empty();
    }
    long principal =
        securitiesAccounts.stream().mapToLong(StoredConnectedAccount::currentBalance).sum();
    return Optional.of(principal);
  }
}
```

- [ ] **Step 6: Write the environment-aware switch**

`EnvironmentAwareAggregateInvestmentPrincipalProvider.java`:

```java
package com.jaedaero.domain.cashflow.service;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class EnvironmentAwareAggregateInvestmentPrincipalProvider
    implements AggregateInvestmentPrincipalProvider {

  private final AggregateInvestmentPrincipalProvider localProvider;
  private final AggregateInvestmentPrincipalProvider codefProvider;
  private final String appEnvironment;

  public EnvironmentAwareAggregateInvestmentPrincipalProvider(
      @Qualifier("localAggregateInvestmentPrincipalProvider") AggregateInvestmentPrincipalProvider localProvider,
      @Qualifier("codefAggregateInvestmentPrincipalProvider") AggregateInvestmentPrincipalProvider codefProvider,
      @Value("${app.environment:production}") String appEnvironment) {
    this.localProvider = localProvider;
    this.codefProvider = codefProvider;
    this.appEnvironment = appEnvironment;
  }

  @Override
  public Optional<Long> resolveLinkedPrincipal(long userId) {
    return isLocal() ? localProvider.resolveLinkedPrincipal(userId) : codefProvider.resolveLinkedPrincipal(userId);
  }

  private boolean isLocal() {
    return "local".equalsIgnoreCase(appEnvironment);
  }
}
```

- [ ] **Step 7: Run test to verify it passes**

Run: `./gradlew test --tests "com.jaedaero.domain.cashflow.service.EnvironmentAwareAggregateInvestmentPrincipalProviderTest"`
Expected: PASS

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/jaedaero/domain/cashflow/service/AggregateInvestmentPrincipalProvider.java \
        src/main/java/com/jaedaero/domain/cashflow/service/CodefAggregateInvestmentPrincipalProvider.java \
        src/main/java/com/jaedaero/domain/cashflow/service/LocalAggregateInvestmentPrincipalProvider.java \
        src/main/java/com/jaedaero/domain/cashflow/service/EnvironmentAwareAggregateInvestmentPrincipalProvider.java \
        src/test/java/com/jaedaero/domain/cashflow/service/EnvironmentAwareAggregateInvestmentPrincipalProviderTest.java
git commit -m "feat: resolve linked brokerage investment principal without requiring a recurring plan"
```

---

### Task 4: Investment principal backfill for unlinked accounts

**Files:**
- Create: `src/main/java/com/jaedaero/domain/cashflow/service/InvestmentPrincipalBackfill.java`
- Test: `src/test/java/com/jaedaero/domain/cashflow/service/InvestmentPrincipalBackfillTest.java`

**Interfaces:**
- Consumes: `DefaultMilitaryPayPolicy.resolve(SoldierType, YearMonth enlistmentMonth, YearMonth month)` (existing)
- Produces: `long estimate(SoldierType soldierType, LocalDate enlistmentDate, LocalDate calculationDate, BigDecimal investmentRatio)` — sums `salary(m) × investmentRatio` for every month from enlistment up to (but not including) the calculation month.

- [ ] **Step 1: Write the failing test**

Create `src/test/java/com/jaedaero/domain/cashflow/service/InvestmentPrincipalBackfillTest.java`:

```java
package com.jaedaero.domain.cashflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jaedaero.domain.auth.common.enums.SoldierType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class InvestmentPrincipalBackfillTest {

  @Test
  void sumsPastMonthlySalaryTimesRatioFromEnlistmentUpToCalculationMonth() {
    DefaultMilitaryPayPolicy payPolicy =
        (soldierType, enlistmentMonth, month) -> new DefaultMilitaryPayPolicy.MilitaryPay("이병", 400_000L);
    InvestmentPrincipalBackfill backfill = new InvestmentPrincipalBackfill(payPolicy);

    long estimate =
        backfill.estimate(
            SoldierType.ARMY,
            LocalDate.of(2026, 1, 15),
            LocalDate.of(2026, 4, 15),
            new BigDecimal("0.1000"));

    // 3 past months (Jan, Feb, Mar) * 400,000 * 10% = 120,000
    assertEquals(120_000L, estimate);
  }

  @Test
  void zeroRatioProducesZeroWithoutCallingThePayPolicy() {
    InvestmentPrincipalBackfill backfill =
        new InvestmentPrincipalBackfill(
            (soldierType, enlistmentMonth, month) -> {
              throw new AssertionError("should not resolve pay when ratio is zero");
            });

    long estimate =
        backfill.estimate(SoldierType.ARMY, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 1), BigDecimal.ZERO);

    assertEquals(0L, estimate);
  }
}
```

`DefaultMilitaryPayPolicy` is a concrete `@Component` class today, not an interface — this test relies on Step 3 extracting an interface so it can be stubbed with a lambda. Confirm this before writing Step 3.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.jaedaero.domain.cashflow.service.InvestmentPrincipalBackfillTest"`
Expected: FAIL — `InvestmentPrincipalBackfill` doesn't exist, and `DefaultMilitaryPayPolicy` can't be used as a functional interface yet.

- [ ] **Step 3: Extract a `MilitaryPayPolicy` interface**

In `DefaultMilitaryPayPolicy.java`, extract the public method into a new interface `MilitaryPayPolicy` in the same package, and have `DefaultMilitaryPayPolicy implements MilitaryPayPolicy`:

```java
package com.jaedaero.domain.cashflow.service;

import com.jaedaero.domain.auth.common.enums.SoldierType;
import java.time.YearMonth;

public interface MilitaryPayPolicy {
  DefaultMilitaryPayPolicy.MilitaryPay resolve(
      SoldierType soldierType, YearMonth enlistmentMonth, YearMonth month);
}
```

Change `DefaultMilitaryPayPolicy`'s class declaration to `public class DefaultMilitaryPayPolicy implements MilitaryPayPolicy` and add `@Override` on its `resolve` method. Leave everything else in that file unchanged (the `MilitaryPay` record stays nested there since `MilitaryPayPolicy` references it as `DefaultMilitaryPayPolicy.MilitaryPay`).

- [ ] **Step 4: Write `InvestmentPrincipalBackfill`**

```java
package com.jaedaero.domain.cashflow.service;

import com.jaedaero.domain.auth.common.enums.SoldierType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import org.springframework.stereotype.Component;

/** 증권계좌 미연동 사용자의 과거 투자원금을 계급별 월급 x 슬라이더 비율로 역산한다. */
@Component
public class InvestmentPrincipalBackfill {

  private final MilitaryPayPolicy militaryPayPolicy;

  public InvestmentPrincipalBackfill(MilitaryPayPolicy militaryPayPolicy) {
    this.militaryPayPolicy = militaryPayPolicy;
  }

  public long estimate(
      SoldierType soldierType,
      LocalDate enlistmentDate,
      LocalDate calculationDate,
      BigDecimal investmentRatio) {
    if (investmentRatio == null || investmentRatio.signum() == 0) {
      return 0L;
    }
    YearMonth enlistmentMonth = YearMonth.from(enlistmentDate);
    YearMonth calculationMonth = YearMonth.from(calculationDate);
    long total = 0L;
    for (YearMonth month = enlistmentMonth; month.isBefore(calculationMonth); month = month.plusMonths(1)) {
      long salary = militaryPayPolicy.resolve(soldierType, enlistmentMonth, month).monthlySalary();
      long backfilled =
          BigDecimal.valueOf(salary)
              .multiply(investmentRatio)
              .setScale(0, java.math.RoundingMode.HALF_UP)
              .longValueExact();
      total = Math.addExact(total, backfilled);
    }
    return total;
  }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew test --tests "com.jaedaero.domain.cashflow.service.InvestmentPrincipalBackfillTest"`
Expected: PASS

Also re-run the full cashflow test suite to make sure extracting `MilitaryPayPolicy` didn't break existing callers of `DefaultMilitaryPayPolicy` (its concrete-class usages elsewhere are unaffected — they use `new DefaultMilitaryPayPolicy(mapper)` directly, not the interface — but Spring wiring for `@Autowired DefaultMilitaryPayPolicy` sites still resolves fine since the concrete class continues to exist and be a `@Component`):

Run: `./gradlew test --tests "com.jaedaero.domain.cashflow.*"`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/jaedaero/domain/cashflow/service/MilitaryPayPolicy.java \
        src/main/java/com/jaedaero/domain/cashflow/service/DefaultMilitaryPayPolicy.java \
        src/main/java/com/jaedaero/domain/cashflow/service/InvestmentPrincipalBackfill.java \
        src/test/java/com/jaedaero/domain/cashflow/service/InvestmentPrincipalBackfillTest.java
git commit -m "feat: backfill past investment principal for unlinked brokerage accounts"
```

---

### Task 5: Migration — drop dual asset fields, add principal snapshot columns

**Files:**
- Create: `sql/migration/20260813_conservative_cashflow_v3.sql`
- Modify: `jaedaero_db_v1.sql` (apply the same shape to the baseline schema file so a fresh DB matches migrated ones — this repo keeps both, per the existing `20260813_align_cashflow_forecast_policy.sql` pattern of a separate migration file plus eventual baseline sync; check whether the project applies migrations against the baseline automatically or expects manual sync before editing `jaedaero_db_v1.sql`, and skip that edit if the team's convention is migration-only)

**Interfaces:**
- Produces: `cashflow_forecast.soldier_saving_principal`, `cashflow_forecast.investment_principal` (both `BIGINT NOT NULL DEFAULT 0`); removes `cashflow_forecast.potential_expected_asset` and `simulation.potential_expected_asset`.

- [ ] **Step 1: Write the migration**

```sql
-- 보수적/잠재 자산 이중값을 단일 "전역 예상 자산"으로 통합하고, 군적금·투자 원금에
-- 기존 잔액을 포함한 총액 스냅샷 컬럼을 추가한다.

ALTER TABLE cashflow_forecast
    ADD COLUMN soldier_saving_principal BIGINT NOT NULL DEFAULT 0
        COMMENT '군적금 총 원금(기존 잔액 + 전역까지 예상 신규 납입)' AFTER expected_investment_amount,
    ADD COLUMN investment_principal BIGINT NOT NULL DEFAULT 0
        COMMENT '투자 총 원금(연동 증권계좌 매입금액 또는 백필 추정치 + 전역까지 예상 신규 납입)'
        AFTER soldier_saving_principal,
    DROP COLUMN potential_expected_asset;

ALTER TABLE cashflow_forecast
    MODIFY COLUMN expected_asset BIGINT NOT NULL DEFAULT 0
        COMMENT '전역 예상 자산(확정분 + 군적금 이자·매칭지원금 + 투자 예상수익)',
    MODIFY COLUMN calculation_policy_version VARCHAR(50) NOT NULL
        DEFAULT 'CONSERVATIVE_CASHFLOW_V3_20260813'
        COMMENT '캐시플로우 계산 정책 버전';

ALTER TABLE simulation
    DROP COLUMN potential_expected_asset,
    MODIFY COLUMN expected_asset BIGINT NOT NULL
        COMMENT '전역 예상 자산(확정분 + 군적금 이자·매칭지원금 + 투자 예상수익)';
```

- [ ] **Step 2: Apply the migration to the local database and verify**

Run: `./gradlew flywayMigrate` (or whatever this project's migration runner command is — check `build.gradle`/`README` for the exact task name if it isn't Flyway; the presence of `sql/migration/*.sql` files with a similar naming convention as the existing `20260813_align_cashflow_forecast_policy.sql` suggests a custom runner — find and reuse it rather than guessing)

Expected: migration applies without error; `DESCRIBE cashflow_forecast;` shows the two new columns and no `potential_expected_asset`; `DESCRIBE simulation;` shows no `potential_expected_asset`.

- [ ] **Step 3: Commit**

```bash
git add sql/migration/20260813_conservative_cashflow_v3.sql
git commit -m "feat: migrate cashflow_forecast and simulation to single expected_asset"
```

(Task 7 and Task 9 update the Java/MyBatis code that reads and writes these columns — this migration will break the build until those land, which is expected for a multi-task feature branch. If this team gates each commit on a green build, hold off on `flywayMigrate` against a shared database until Tasks 6-9 are also done, and only run it locally for verification here.)

---

### Task 6: `CashflowCalculator` — ratio-scaling, single asset, new engine wiring

**Files:**
- Modify: `src/main/java/com/jaedaero/domain/cashflow/service/CashflowCalculator.java`
- Modify: `src/main/java/com/jaedaero/domain/cashflow/service/CashflowForecastCalculation.java`
- Test: `src/test/java/com/jaedaero/domain/cashflow/service/CashflowCalculatorTest.java`

**Interfaces:**
- Consumes: `ConservativeMonthlyCashflowEngine.calculateProjectedBenefit(...)` (Task 2's new signature), `AggregateInvestmentPrincipalProvider.resolveLinkedPrincipal(userId)` (Task 3), `InvestmentPrincipalBackfill.estimate(...)` (Task 4), `input.soldierSavings()` (Task 1 — already on `CashflowInput`, just newly consumed).
- Produces: `CashflowForecastCalculation` drops `potentialExpectedAsset`, adds `soldierSavingPrincipal`/`investmentPrincipal`.

- [ ] **Step 1: Write the failing tests**

Read the full current `CashflowCalculatorTest.java` first (it's already been modified once for the V2 dual-field policy — you're changing it again for V3). Replace every assertion on `.potentialExpectedAsset()` with removal, and add new assertions for `.soldierSavingPrincipal()`/`.investmentPrincipal()`. Add these new test cases:

```java
  @Test
  void appliedStrategySpendingAndInvestmentScaleWithSalaryButSavingStaysFixed() {
    // enlistment far enough back that a promotion happens partway through the horizon;
    // use the fixture's existing promotion boundaries (check DefaultMilitaryPayPolicy/military_pay_policy
    // test data for exact rank-change months already used elsewhere in this test file, and pick an
    // enlistmentDate/dischargeDate pair that spans a promotion, matching the file's existing fixture style)
    CashflowInput input =
        new CashflowInput(
            0L, 50_000_000L, 0L, SoldierType.ARMY,
            LocalDate.of(2026, 1, 1), LocalDate.of(2027, 6, 30),
            List.of(),
            new AppliedCashflowStrategy(1L, 100_000L, 0L, 100_000L, BigDecimal.ZERO));
    CashflowForecastCalculation result = calculator().calculate(input, LocalDate.of(2026, 1, 1));

    CashflowForecastMonthCalculation firstMonth = result.months().get(0);
    CashflowForecastMonthCalculation laterMonth =
        result.months().get(result.months().size() - 1);
    // spending/investment ratio to first-month salary stays constant; absolute amount grows
    // with rank if laterMonth's salary is higher than firstMonth's (assert using this test file's
    // known fixture pay policy values instead of guessing numbers)
    assertTrue(
        laterMonth.expectedInvestmentAmount() >= firstMonth.expectedInvestmentAmount(),
        "investment amount should not shrink as salary rises");
  }

  @Test
  void appliedStrategyPathHasNoSingleAssetPotentialSplit() {
    CashflowInput input =
        new CashflowInput(
            0L, 50_000_000L, 0L, SoldierType.ARMY,
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30),
            List.of(),
            new AppliedCashflowStrategy(1L, 0L, 0L, 0L, BigDecimal.ZERO));
    CashflowForecastCalculation result = calculator().calculate(input, LocalDate.of(2026, 1, 1));

    // no potentialExpectedAsset accessor should exist on the record at all (compile-time check —
    // if this file still compiles with a call to result.potentialExpectedAsset(), Step 3 isn't done)
    assertNotNull(result.expectedAsset());
  }
```

Adjust the exact salary fixture numbers to match whatever `military_pay_policy` test data this file's `calculator()` helper already wires up — read the rest of the file for that before finalizing these two tests' literals.

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "com.jaedaero.domain.cashflow.service.CashflowCalculatorTest"`
Expected: FAIL — compile errors on `.potentialExpectedAsset()` calls you removed being referenced elsewhere, and the new fields not existing yet.

- [ ] **Step 3: Update `CashflowForecastCalculation`**

```java
package com.jaedaero.domain.cashflow.service;

import java.time.LocalDate;
import java.util.List;

/** 순수 월별 현금흐름 계산이 반환하는 결과입니다. */
public record CashflowForecastCalculation(
    long expectedSalary,
    long expectedSpending,
    long expectedSavingAmount,
    long expectedInvestmentAmount,
    long expectedAsset,
    long soldierSavingPrincipal,
    long soldierSavingInterest,
    long governmentMatchingSupport,
    long investmentPrincipal,
    long expectedInvestmentReturn,
    long projectedBenefitAmount,
    String calculationPolicyVersion,
    long monthlySpendingLimit,
    double achievementRate,
    LocalDate financialDischargeDate,
    List<CashflowForecastMonthCalculation> months) {}
```

- [ ] **Step 4: Add `userId` to `CashflowInput`, then rewrite `CashflowCalculator`**

`CashflowInput` doesn't carry a `userId` today, but `AggregateInvestmentPrincipalProvider.resolveLinkedPrincipal(long userId)` (Task 3) needs one. Add `long userId` as the first field of the record:

```java
public record CashflowInput(
    long userId,
    long baseAsset,
    long targetAmount,
    long monthlySpendingAverage,
    SoldierType soldierType,
    LocalDate enlistmentDate,
    LocalDate dischargeDate,
    List<SoldierSavingInput> soldierSavings,
    AppliedCashflowStrategy appliedStrategy) {

  public CashflowInput(
      long userId,
      long baseAsset,
      long targetAmount,
      long monthlySpendingAverage,
      SoldierType soldierType,
      LocalDate enlistmentDate,
      LocalDate dischargeDate,
      List<SoldierSavingInput> soldierSavings) {
    this(
        userId, baseAsset, targetAmount, monthlySpendingAverage, soldierType, enlistmentDate,
        dischargeDate, soldierSavings, null);
  }
}
```

(this drops the old 6-arg convenience constructor with no `soldierSavings`/`userId` — every call site must now supply `userId` and `soldierSavings` explicitly, even if the list is `List.of()`)

Update `DbCashflowInputProvider.load` to pass `userId` as the new first argument to `new CashflowInput(...)`. Then find every other `new CashflowInput(` call site with `grep -rn "new CashflowInput(" src/main src/test` and fix each one's argument list — this will include `CashflowCalculatorTest`'s fixtures from Step 1.

With `input.userId()` available, add the new dependencies to `CashflowCalculator`'s constructor:

```java
  private final DefaultMilitaryPayPolicy militaryPayPolicy;
  private final ConservativeMonthlyCashflowEngine cashflowEngine;
  private final AggregateInvestmentPrincipalProvider investmentPrincipalProvider;
  private final InvestmentPrincipalBackfill investmentPrincipalBackfill;

  @Autowired
  public CashflowCalculator(
      DefaultMilitaryPayPolicy militaryPayPolicy,
      ConservativeMonthlyCashflowEngine cashflowEngine,
      AggregateInvestmentPrincipalProvider investmentPrincipalProvider,
      InvestmentPrincipalBackfill investmentPrincipalBackfill) {
    this.militaryPayPolicy = militaryPayPolicy;
    this.cashflowEngine = cashflowEngine;
    this.investmentPrincipalProvider = investmentPrincipalProvider;
    this.investmentPrincipalBackfill = investmentPrincipalBackfill;
  }

  /** 단위 테스트와 독립 계산 호출의 하위 호환용 생성자입니다. */
  public CashflowCalculator(DefaultMilitaryPayPolicy militaryPayPolicy) {
    this(
        militaryPayPolicy,
        new ConservativeMonthlyCashflowEngine(),
        userId -> java.util.Optional.empty(),
        new InvestmentPrincipalBackfill(militaryPayPolicy));
  }
```

Change the month loop (currently lines 66-129) to scale spending/investment by ratio when a strategy is applied, and collect contribution dates:

```java
    long asset = input.baseAsset();
    long expectedSalary = 0L;
    long expectedSpending = 0L;
    long expectedSavingAmount = 0L;
    long expectedInvestmentAmount = 0L;
    long firstMonthSpendingLimit = 0L;
    LocalDate financialDischargeDate = asset >= input.targetAmount() ? calculationDate : null;
    List<CashflowForecastMonthCalculation> months = new ArrayList<>();
    List<Long> savingContributions = new ArrayList<>();
    List<LocalDate> savingContributionDates = new ArrayList<>();
    List<Long> investmentContributions = new ArrayList<>();
    int totalMonths = (int) ChronoUnit.MONTHS.between(startMonth, dischargeMonth) + 1;
    boolean hasAppliedStrategy = input.appliedStrategy() != null;

    long firstMonthSalary = 0L;
    BigDecimal spendingRatio = BigDecimal.ZERO;
    BigDecimal investmentRatio = BigDecimal.ZERO;
    if (hasAppliedStrategy) {
      firstMonthSalary =
          militaryPayPolicy
              .resolve(input.soldierType(), YearMonth.from(input.enlistmentDate()), startMonth)
              .monthlySalary();
      if (firstMonthSalary > 0) {
        spendingRatio =
            BigDecimal.valueOf(input.appliedStrategy().monthlySpendingAmount())
                .divide(BigDecimal.valueOf(firstMonthSalary), 10, RoundingMode.HALF_UP);
        investmentRatio =
            BigDecimal.valueOf(input.appliedStrategy().monthlyInvestmentAmount())
                .divide(BigDecimal.valueOf(firstMonthSalary), 10, RoundingMode.HALF_UP);
      }
    }

    for (int index = 0; index < totalMonths; index++) {
      YearMonth month = startMonth.plusMonths(index);
      int remainingMonths = totalMonths - index;
      DefaultMilitaryPayPolicy.MilitaryPay pay =
          militaryPayPolicy.resolve(input.soldierType(), YearMonth.from(input.enlistmentDate()), month);
      long requiredSaving = requiredSaving(input.targetAmount(), asset, remainingMonths);

      long spending;
      long monthlySaving;
      long monthlyInvestment;
      if (hasAppliedStrategy) {
        spending = scale(spendingRatio, pay.monthlySalary());
        monthlySaving = input.appliedStrategy().monthlySavingAmount();
        monthlyInvestment = scale(investmentRatio, pay.monthlySalary());
        validateAppliedStrategy(spending, monthlySaving, monthlyInvestment, pay.monthlySalary());
      } else {
        spending = Math.max(0L, input.monthlySpendingAverage());
        monthlySaving = Math.min(DEFAULT_MONTHLY_SAVING_AMOUNT, pay.monthlySalary());
        monthlyInvestment =
            Math.max(
                0L,
                Math.min(
                    pay.monthlySalary() - spending - monthlySaving, requiredSaving - monthlySaving));
      }
      long spendingLimit = Math.max(0L, pay.monthlySalary() - requiredSaving);

      long assetBeforeMonth = asset;
      ConservativeMonthlyCashflowEngine.MonthProjection projection =
          cashflowEngine.project(
              assetBeforeMonth,
              pay.monthlySalary(),
              spending,
              input.targetAmount(),
              calculationDate,
              input.dischargeDate(),
              month);
      asset = projection.endingAsset();
      expectedSalary += pay.monthlySalary();
      expectedSpending += spending;
      expectedSavingAmount += monthlySaving;
      expectedInvestmentAmount += monthlyInvestment;
      savingContributions.add(monthlySaving);
      savingContributionDates.add(month.atDay(1));
      investmentContributions.add(monthlyInvestment);
      if (index == 0) firstMonthSpendingLimit = spendingLimit;
      if (financialDischargeDate == null && projection.targetReachedDate() != null) {
        financialDischargeDate = projection.targetReachedDate();
      }
      months.add(
          new CashflowForecastMonthCalculation(
              month.atDay(1), pay.rankName(), pay.monthlySalary(), monthlySaving, monthlyInvestment,
              spending, asset));
    }

    BigDecimal investmentAnnualReturnRate =
        hasAppliedStrategy ? input.appliedStrategy().expectedReturnRate() : BigDecimal.ZERO;
    long existingInvestmentPrincipal =
        investmentPrincipalProvider
            .resolveLinkedPrincipal(input.userId())
            .orElseGet(
                () ->
                    investmentPrincipalBackfill.estimate(
                        input.soldierType(), input.enlistmentDate(), calculationDate, investmentRatio));
    ConservativeMonthlyCashflowEngine.ProjectedBenefit benefit =
        cashflowEngine.calculateProjectedBenefit(
            input.soldierSavings(),
            calculationDate,
            savingContributions,
            savingContributionDates,
            input.dischargeDate(),
            existingInvestmentPrincipal,
            investmentContributions,
            investmentAnnualReturnRate);

    return new CashflowForecastCalculation(
        expectedSalary,
        expectedSpending,
        expectedSavingAmount,
        expectedInvestmentAmount,
        asset,
        benefit.soldierSavingPrincipal(),
        benefit.soldierSavingInterest(),
        benefit.governmentMatchingSupport(),
        benefit.investmentPrincipal(),
        benefit.expectedInvestmentReturn(),
        benefit.projectedBenefitAmount(),
        ConservativeMonthlyCashflowEngine.CALCULATION_POLICY_VERSION,
        firstMonthSpendingLimit,
        achievementRate(asset, input.targetAmount()),
        financialDischargeDate,
        List.copyOf(months));
```

Add the two small helpers used above:

```java
  private long scale(BigDecimal ratio, long referenceSalary) {
    return ratio.multiply(BigDecimal.valueOf(referenceSalary))
        .setScale(0, RoundingMode.HALF_UP)
        .longValueExact();
  }

  private void validateAppliedStrategy(
      long spending, long saving, long investment, long referenceMonthlyIncome) {
    if (spending < 0
        || saving < 0
        || saving > DEFAULT_MONTHLY_SAVING_AMOUNT
        || investment < 0) {
      throw new CashflowException(
          CashflowErrorCode.INPUT_NOT_READY, "활성 AI 전략의 월 배분 금액이 유효하지 않습니다.");
    }
    try {
      long allocated = Math.addExact(Math.addExact(spending, saving), investment);
      if (allocated > referenceMonthlyIncome) {
        throw new CashflowException(
            CashflowErrorCode.INPUT_NOT_READY, "활성 AI 전략의 월 배분 합계가 해당 월 군 월급을 초과합니다.");
      }
    } catch (ArithmeticException exception) {
      throw new CashflowException(
          CashflowErrorCode.INPUT_NOT_READY, "활성 AI 전략의 월 배분 합계를 계산할 수 없습니다.");
    }
  }
```

Delete the old `validateAppliedStrategy(AppliedCashflowStrategy, long)` overload — it's fully replaced by the new one above. Also delete the old `expected_return_rate` null-check that lived inside it (`strategy.expectedReturnRate() == null`) — move that single check to right after `hasAppliedStrategy` is determined, since it's a strategy-shape check, not a per-month amount check:

```java
    if (hasAppliedStrategy
        && (input.appliedStrategy().expectedReturnRate() == null
            || input.appliedStrategy().expectedReturnRate().signum() < 0)) {
      throw new CashflowException(
          CashflowErrorCode.INPUT_NOT_READY, "활성 AI 전략의 월 배분 금액이 유효하지 않습니다.");
    }
```
(place this right after the `boolean hasAppliedStrategy = input.appliedStrategy() != null;` line)

Add the needed imports: `java.math.RoundingMode` is already imported; add nothing else new beyond what's already at the top of the file.

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew test --tests "com.jaedaero.domain.cashflow.service.CashflowCalculatorTest"`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/jaedaero/domain/cashflow/service/CashflowCalculator.java \
        src/main/java/com/jaedaero/domain/cashflow/service/CashflowForecastCalculation.java \
        src/main/java/com/jaedaero/domain/cashflow/service/CashflowInput.java \
        src/main/java/com/jaedaero/domain/cashflow/service/DbCashflowInputProvider.java \
        src/test/java/com/jaedaero/domain/cashflow/service/CashflowCalculatorTest.java
git commit -m "feat: scale applied-strategy spending/investment with salary, wire investment principal into cashflow calculator"
```

---

### Task 7: Update `CashflowForecastVo`/`CashflowForecastResponse`/`CashflowServiceImpl`/mapper for the new fields

**Files:**
- Modify: `src/main/java/com/jaedaero/domain/cashflow/vo/CashflowForecastVo.java`
- Modify: `src/main/java/com/jaedaero/domain/cashflow/dto/CashflowForecastResponse.java`
- Modify: `src/main/java/com/jaedaero/domain/cashflow/service/impl/CashflowServiceImpl.java`
- Modify: `src/main/resources/mapper/cashflow/CashflowMapper.xml:73-113`
- Test: `src/test/java/com/jaedaero/domain/cashflow/service/CashflowServiceImplTest.java`

**Interfaces:**
- Consumes: `CashflowForecastCalculation` (Task 6's new shape)
- Produces: `CashflowForecastResponse` with `soldierSavingPrincipal`, `investmentPrincipal` fields added and `potentialExpectedAsset`, `returnsIncludedInExpectedAsset` removed.

- [ ] **Step 1: Update the failing test**

In `CashflowServiceImplTest.java`, update `generatePersistsSummaryAndMonthlyRows_thenLatestReturnsThem` (lines 27-55): remove the `assertEquals(..., generated.getPotentialExpectedAsset())` and `assertEquals(false, generated.getReturnsIncludedInExpectedAsset())` lines, and add:

```java
    assertEquals(2_200_000L, generated.getInvestmentPrincipal());
    assertEquals(8_000_000L, generated.getSoldierSavingPrincipal());
```

(these numbers assume the existing `InMemoryCashflowMapper`/`CashflowInputSourceVo` fixture in this file has no `soldier_saving` rows and no linked securities account, i.e. `soldierSavingPrincipal`/`investmentPrincipal` equal the future-only sums, same as `expectedSavingAmount`/`expectedInvestmentAmount` — read the fixture in this file first to confirm those exact numbers before finalizing; if the fixture already includes a mocked securities balance the numbers will differ)

The `latest.getPotentialExpectedAsset()` assertion at the end of that test should also be deleted.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.jaedaero.domain.cashflow.service.CashflowServiceImplTest"`
Expected: FAIL — compile errors, methods don't exist yet.

- [ ] **Step 3: Update `CashflowForecastVo`**

```java
package com.jaedaero.domain.cashflow.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashflowForecastVo {
  private Long forecastId;
  private Long userId;
  private Long baseAsset;
  private Long expectedSalary;
  private Long expectedSpending;
  private Long expectedSavingAmount;
  private Long expectedInvestmentAmount;
  private Long expectedAsset;
  private Long soldierSavingPrincipal;
  private Long soldierSavingInterest;
  private Long governmentMatchingSupport;
  private Long investmentPrincipal;
  private Long expectedInvestmentReturn;
  private Long projectedBenefitAmount;
  private String calculationPolicyVersion;
  private Long monthlySpendingLimit;
  private BigDecimal achievementRate;
  private LocalDate financialDischargeDate;
  private String policyVersion;
  private LocalDateTime generatedAt;
}
```

- [ ] **Step 4: Update `CashflowForecastResponse`**

```java
package com.jaedaero.domain.cashflow.dto;

import com.jaedaero.domain.cashflow.vo.CashflowForecastVo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CashflowForecastResponse {
  private final Long forecastId;
  private final Long baseAsset;
  private final Long expectedSalary;
  private final Long expectedSpending;
  private final Long expectedSavingAmount;
  private final Long expectedInvestmentAmount;
  private final Long expectedAsset;
  private final Long soldierSavingPrincipal;
  private final Long soldierSavingInterest;
  private final Long governmentMatchingSupport;
  private final Long investmentPrincipal;
  private final Long expectedInvestmentReturn;
  private final Long projectedBenefitAmount;
  private final String calculationPolicyVersion;
  private final Long monthlySpendingLimit;
  private final BigDecimal achievementRate;
  private final LocalDate financialDischargeDate;
  private final List<CashflowForecastMonthResponse> months;

  public static CashflowForecastResponse from(
      CashflowForecastVo forecast, List<CashflowForecastMonthResponse> months) {
    return CashflowForecastResponse.builder()
        .forecastId(forecast.getForecastId())
        .baseAsset(forecast.getBaseAsset())
        .expectedSalary(forecast.getExpectedSalary())
        .expectedSpending(forecast.getExpectedSpending())
        .expectedSavingAmount(forecast.getExpectedSavingAmount())
        .expectedInvestmentAmount(forecast.getExpectedInvestmentAmount())
        .expectedAsset(forecast.getExpectedAsset())
        .soldierSavingPrincipal(forecast.getSoldierSavingPrincipal())
        .soldierSavingInterest(forecast.getSoldierSavingInterest())
        .governmentMatchingSupport(forecast.getGovernmentMatchingSupport())
        .investmentPrincipal(forecast.getInvestmentPrincipal())
        .expectedInvestmentReturn(forecast.getExpectedInvestmentReturn())
        .projectedBenefitAmount(forecast.getProjectedBenefitAmount())
        .calculationPolicyVersion(forecast.getCalculationPolicyVersion())
        .monthlySpendingLimit(forecast.getMonthlySpendingLimit())
        .achievementRate(forecast.getAchievementRate())
        .financialDischargeDate(forecast.getFinancialDischargeDate())
        .months(months)
        .build();
  }
}
```

- [ ] **Step 5: Update `CashflowServiceImpl.generate`**

In the `CashflowForecastVo.builder()` chain (lines 66-85), replace:

```java
            .soldierSavingInterest(calculation.soldierSavingInterest())
            .governmentMatchingSupport(calculation.governmentMatchingSupport())
            .expectedInvestmentReturn(calculation.expectedInvestmentReturn())
            .projectedBenefitAmount(calculation.projectedBenefitAmount())
            .potentialExpectedAsset(calculation.potentialExpectedAsset())
```

with:

```java
            .soldierSavingPrincipal(calculation.soldierSavingPrincipal())
            .soldierSavingInterest(calculation.soldierSavingInterest())
            .governmentMatchingSupport(calculation.governmentMatchingSupport())
            .investmentPrincipal(calculation.investmentPrincipal())
            .expectedInvestmentReturn(calculation.expectedInvestmentReturn())
            .projectedBenefitAmount(calculation.projectedBenefitAmount())
```

- [ ] **Step 6: Update the mapper XML**

`insertForecast` (lines 73-88):

```xml
    <insert id="insertForecast" parameterType="com.jaedaero.domain.cashflow.vo.CashflowForecastVo"
            useGeneratedKeys="true" keyProperty="forecastId">
        INSERT INTO cashflow_forecast (
            user_id, base_asset, expected_salary, expected_spending, expected_saving_amount,
            expected_investment_amount, expected_asset, soldier_saving_principal,
            soldier_saving_interest, government_matching_support, investment_principal,
            expected_investment_return, projected_benefit_amount, calculation_policy_version,
            monthly_spending_limit, achievement_rate, financial_discharge_date, policy_version
        ) VALUES (
            #{userId}, #{baseAsset}, #{expectedSalary}, #{expectedSpending}, #{expectedSavingAmount},
            #{expectedInvestmentAmount}, #{expectedAsset}, #{soldierSavingPrincipal},
            #{soldierSavingInterest}, #{governmentMatchingSupport}, #{investmentPrincipal},
            #{expectedInvestmentReturn}, #{projectedBenefitAmount}, #{calculationPolicyVersion},
            #{monthlySpendingLimit}, #{achievementRate}, #{financialDischargeDate}, #{policyVersion}
        )
    </insert>
```

`findLatestForecastByUserId` (lines 102-113):

```xml
    <select id="findLatestForecastByUserId" resultType="com.jaedaero.domain.cashflow.vo.CashflowForecastVo">
        SELECT forecast_id, user_id, base_asset, expected_salary, expected_spending,
               expected_saving_amount, expected_investment_amount, expected_asset,
               soldier_saving_principal, soldier_saving_interest, government_matching_support,
               investment_principal, expected_investment_return, projected_benefit_amount,
               calculation_policy_version, monthly_spending_limit, achievement_rate,
               financial_discharge_date, policy_version, generated_at
        FROM cashflow_forecast
        WHERE user_id = #{userId}
        ORDER BY generated_at DESC, forecast_id DESC
        LIMIT 1
    </select>
```

- [ ] **Step 7: Run tests to verify they pass**

Run: `./gradlew test --tests "com.jaedaero.domain.cashflow.*"`
Expected: PASS

Also run against a real (migrated) local database if this project has an integration-test profile — grep `src/test` for `@SpringBootTest` cashflow tests hitting the actual `cashflow_forecast` table and run those too, to catch any MyBatis column-mismatch the unit tests with `InMemoryCashflowMapper` can't catch.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/jaedaero/domain/cashflow/vo/CashflowForecastVo.java \
        src/main/java/com/jaedaero/domain/cashflow/dto/CashflowForecastResponse.java \
        src/main/java/com/jaedaero/domain/cashflow/service/impl/CashflowServiceImpl.java \
        src/main/resources/mapper/cashflow/CashflowMapper.xml \
        src/test/java/com/jaedaero/domain/cashflow/service/CashflowServiceImplTest.java
git commit -m "feat: expose single expected_asset with principal breakdown in cashflow forecast API"
```

---

### Task 8: `SimulationCalculator` — always-on ratio-scaling, new engine/provider wiring

**Files:**
- Modify: `src/main/java/com/jaedaero/domain/simulation/service/SimulationCalculator.java`
- Modify: `src/main/java/com/jaedaero/domain/simulation/service/SimulationInput.java`
- Modify: `src/main/java/com/jaedaero/domain/simulation/service/SimulationCalculationResult.java`
- Modify: `src/main/java/com/jaedaero/domain/simulation/service/CashflowSimulationInputProvider.java`
- Modify: `src/main/java/com/jaedaero/domain/cashflow/dto/CashflowCalculationInputResponse.java`
- Test: `src/test/java/com/jaedaero/domain/simulation/service/SimulationServiceImplTest.java` (and any `SimulationCalculatorTest` if one exists — check `src/test/java/com/jaedaero/domain/simulation/service/`)

**Interfaces:**
- Consumes: same `ConservativeMonthlyCashflowEngine`/`AggregateInvestmentPrincipalProvider`/`InvestmentPrincipalBackfill` as Task 6.
- Produces: `SimulationCalculationResult` drops `potentialExpectedAsset`.

Unlike `CashflowCalculator`, ratio-scaling here applies unconditionally — a `SimulationRequest`'s `monthlySpendingAmount`/`monthlyInvestmentAmount` are always the user's explicitly-set values for the *first* projected month, and this task scales them forward the same way.

- [ ] **Step 1: Check for an existing `SimulationCalculatorTest`**

Run: `find src/test/java/com/jaedaero/domain/simulation -iname "SimulationCalculatorTest.java"`

If it exists, read it fully and follow its fixture-building pattern for the new tests below. If it doesn't exist, add the new tests directly to `SimulationServiceImplTest.java` following that file's existing pattern (it already builds `SimulationInput`/`SimulationRequest` fixtures — read that file fully before writing Step 1's test).

- [ ] **Step 2: Write the failing tests**

Add (adapting names/fixture style to whichever file Step 1 pointed you to):

```java
  @Test
  void investmentAndSpendingScaleWithSalaryAcrossAPromotion() {
    // pick an enlistmentDate/dischargeDate pair spanning a rank change in this test file's
    // existing military_pay_policy fixture (same fixture used by CashflowCalculatorTest —
    // reuse the same enlistment/discharge dates so the promotion boundary is known)
    SimulationInput input = /* build using this file's existing helper, with soldierSavings = List.of() */;
    SimulationRequest request = new SimulationRequest();
    request.setMonthlySpendingAmount(100_000L);
    request.setMonthlySavingAmount(0L);
    request.setMonthlyInvestmentAmount(100_000L);
    request.setExpectedReturnRate(BigDecimal.ZERO);

    SimulationCalculationResult result =
        calculator().calculate(input, request, /* today matching enlistmentDate's month */);

    // assert later-month investment/spending amounts are >= the first month's, using this
    // file's actual monthly breakdown accessor if SimulationCalculationResult exposes one,
    // or by re-deriving expectedInvestmentReturn's principal growth as a proxy if it doesn't
  }

  @Test
  void resultHasNoPotentialExpectedAssetField() {
    // compile-time check: SimulationCalculationResult.potentialExpectedAsset() must not exist.
    // If this test file still compiles referencing it, Step 4 below isn't done.
  }
```

Replace the placeholder comments with real assertions once you've read the existing test file's fixture-building helpers — this plan doesn't have those helpers' exact signatures in front of it, so don't invent numbers; derive them from the same `military_pay_policy` seed data `CashflowCalculatorTest` already uses (same repo, same fixture, so the numbers must be consistent between the two test files).

- [ ] **Step 3: Run tests to verify they fail**

Run: `./gradlew test --tests "com.jaedaero.domain.simulation.service.*"`
Expected: FAIL

- [ ] **Step 4: Update `SimulationInput` and `SimulationCalculationResult`**

```java
package com.jaedaero.domain.simulation.service;

import com.jaedaero.domain.auth.common.enums.SoldierType;
import com.jaedaero.domain.cashflow.service.AppliedCashflowStrategy;
import com.jaedaero.domain.cashflow.service.SoldierSavingInput;
import java.time.LocalDate;
import java.util.List;

/** 시뮬레이션 계산에 필요한 도메인 입력값. */
public record SimulationInput(
    long userId,
    long baseAsset,
    long targetAmount,
    long monthlySpendingAverage,
    SoldierType soldierType,
    LocalDate enlistmentDate,
    LocalDate dischargeDate,
    List<SoldierSavingInput> soldierSavings,
    AppliedCashflowStrategy appliedStrategy) {

  public SimulationInput(
      long userId,
      long baseAsset,
      long targetAmount,
      long monthlySpendingAverage,
      SoldierType soldierType,
      LocalDate enlistmentDate,
      LocalDate dischargeDate,
      List<SoldierSavingInput> soldierSavings) {
    this(
        userId, baseAsset, targetAmount, monthlySpendingAverage, soldierType, enlistmentDate,
        dischargeDate, soldierSavings, null);
  }
}
```

(this drops the old no-`soldierSavings` convenience constructor entirely — every call site must now supply the list explicitly, even if it's `List.of()`; find every `new SimulationInput(` call site with `grep -rn "new SimulationInput(" src/` and fix each one, including test fixtures)

```java
package com.jaedaero.domain.simulation.service;

import java.time.LocalDate;

public record SimulationCalculationResult(
    long expectedAsset,
    LocalDate financialDischargeDate,
    int calculationMonths,
    long baseAsset,
    long expectedSalary,
    long expectedSpending,
    long soldierSavingPrincipal,
    long soldierSavingInterest,
    long governmentMatchingSupport,
    long investmentPrincipal,
    long expectedInvestmentReturn,
    long unallocatedPrincipal,
    String calculationPolicyVersion) {}
```

- [ ] **Step 5: Update `CashflowCalculationInputResponse` to carry `soldierSavings` and `userId`**

```java
package com.jaedaero.domain.cashflow.dto;

import com.jaedaero.domain.auth.common.enums.SoldierType;
import com.jaedaero.domain.cashflow.service.AppliedCashflowStrategy;
import com.jaedaero.domain.cashflow.service.CashflowInput;
import com.jaedaero.domain.cashflow.service.SoldierSavingInput;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** 다른 도메인이 캐시플로우 계산 기준을 읽을 때 사용하는 읽기 전용 계약이다. */
@Getter
@Builder
public class CashflowCalculationInputResponse {
  private final long userId;
  private final long baseAsset;
  private final long targetAmount;
  private final long monthlySpendingAverage;
  private final SoldierType soldierType;
  private final LocalDate enlistmentDate;
  private final LocalDate dischargeDate;
  private final List<SoldierSavingInput> soldierSavings;
  private final AppliedCashflowStrategy appliedStrategy;

  public static CashflowCalculationInputResponse from(CashflowInput input) {
    return builder()
        .userId(input.userId())
        .baseAsset(input.baseAsset())
        .targetAmount(input.targetAmount())
        .monthlySpendingAverage(input.monthlySpendingAverage())
        .soldierType(input.soldierType())
        .enlistmentDate(input.enlistmentDate())
        .dischargeDate(input.dischargeDate())
        .soldierSavings(input.soldierSavings())
        .appliedStrategy(input.appliedStrategy())
        .build();
  }
}
```

(this assumes Task 6 already added `userId` to `CashflowInput` — if you're executing Task 8 before Task 6 for some reason, add it here first)

- [ ] **Step 6: Update `CashflowSimulationInputProvider`**

```java
  @Override
  public SimulationInput load(long userId) {
    try {
      CashflowCalculationInputResponse source = cashflowService.getCalculationInput(userId);
      return new SimulationInput(
          source.getUserId(),
          source.getBaseAsset(),
          source.getTargetAmount(),
          source.getMonthlySpendingAverage(),
          source.getSoldierType(),
          source.getEnlistmentDate(),
          source.getDischargeDate(),
          source.getSoldierSavings(),
          source.getAppliedStrategy());
    } catch (CashflowException exception) {
      throw new SimulationException(
          SimulationErrorCode.INPUT_NOT_READY,
          "시뮬레이션에 필요한 캐시플로우, 목표 또는 복무 정보가 없습니다.");
    }
  }
```

- [ ] **Step 7: Rewrite `SimulationCalculator.calculate`**

Add the same three new constructor dependencies as Task 6's `CashflowCalculator` (`AggregateInvestmentPrincipalProvider`, `InvestmentPrincipalBackfill`), following the same pattern:

```java
  private final DefaultMilitaryPayPolicy militaryPayPolicy;
  private final ConservativeMonthlyCashflowEngine cashflowEngine;
  private final AggregateInvestmentPrincipalProvider investmentPrincipalProvider;
  private final InvestmentPrincipalBackfill investmentPrincipalBackfill;

  @Autowired
  public SimulationCalculator(
      DefaultMilitaryPayPolicy militaryPayPolicy,
      ConservativeMonthlyCashflowEngine cashflowEngine,
      AggregateInvestmentPrincipalProvider investmentPrincipalProvider,
      InvestmentPrincipalBackfill investmentPrincipalBackfill) {
    this.militaryPayPolicy = militaryPayPolicy;
    this.cashflowEngine = cashflowEngine;
    this.investmentPrincipalProvider = investmentPrincipalProvider;
    this.investmentPrincipalBackfill = investmentPrincipalBackfill;
  }
```

Replace the body of `calculate` (currently lines 50-121). Keep the `emptyResult` early-return path (lines 56-57) as-is except for dropping `potentialExpectedAsset` from `emptyResult`'s own return (Step 8 below). Replace the main body with ratio-scaling analogous to Task 6:

```java
    long asset = input.baseAsset();

    YearMonth startMonth = YearMonth.from(calculationDate);
    YearMonth dischargeMonth = YearMonth.from(input.dischargeDate());
    if (calculationDate.isAfter(input.dischargeDate())) {
      return emptyResult(input, asset >= input.targetAmount() ? calculationDate : null);
    }

    LocalDate financialDischargeDate = asset >= input.targetAmount() ? calculationDate : null;
    YearMonth enlistmentMonth = YearMonth.from(input.enlistmentDate());
    int totalMonths = (int) ChronoUnit.MONTHS.between(startMonth, dischargeMonth) + 1;
    long expectedSalary = 0L;
    long expectedSpending = 0L;
    long unallocatedPrincipal = 0L;
    List<Long> savingContributions = new java.util.ArrayList<>();
    List<LocalDate> savingContributionDates = new java.util.ArrayList<>();
    List<Long> investmentContributions = new java.util.ArrayList<>();

    long firstMonthSalary =
        militaryPayPolicy.resolve(input.soldierType(), enlistmentMonth, startMonth).monthlySalary();
    BigDecimal spendingRatio =
        firstMonthSalary > 0
            ? BigDecimal.valueOf(request.getMonthlySpendingAmount())
                .divide(BigDecimal.valueOf(firstMonthSalary), 10, java.math.RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
    BigDecimal investmentRatio =
        firstMonthSalary > 0
            ? BigDecimal.valueOf(request.getMonthlyInvestmentAmount())
                .divide(BigDecimal.valueOf(firstMonthSalary), 10, java.math.RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

    for (int index = 0; index < totalMonths; index++) {
      YearMonth month = startMonth.plusMonths(index);
      long salary = militaryPayPolicy.resolve(input.soldierType(), enlistmentMonth, month).monthlySalary();
      long spending = scale(spendingRatio, salary);
      long investment = scale(investmentRatio, salary);
      long assetBeforeMonth = asset;

      ConservativeMonthlyCashflowEngine.MonthProjection projection =
          cashflowEngine.project(
              assetBeforeMonth, salary, spending, input.targetAmount(), calculationDate,
              input.dischargeDate(), month);
      asset = projection.endingAsset();
      expectedSalary = Math.addExact(expectedSalary, salary);
      expectedSpending = Math.addExact(expectedSpending, spending);
      unallocatedPrincipal =
          Math.addExact(
              unallocatedPrincipal,
              Math.subtractExact(
                  Math.subtractExact(Math.subtractExact(salary, spending), request.getMonthlySavingAmount()),
                  investment));
      savingContributions.add(request.getMonthlySavingAmount());
      savingContributionDates.add(month.atDay(1));
      investmentContributions.add(investment);
      if (financialDischargeDate == null && projection.targetReachedDate() != null) {
        financialDischargeDate = projection.targetReachedDate();
      }
    }

    long existingInvestmentPrincipal =
        investmentPrincipalProvider
            .resolveLinkedPrincipal(input.userId())
            .orElseGet(
                () ->
                    investmentPrincipalBackfill.estimate(
                        input.soldierType(), input.enlistmentDate(), calculationDate, investmentRatio));
    ConservativeMonthlyCashflowEngine.ProjectedBenefit benefit =
        cashflowEngine.calculateProjectedBenefit(
            input.soldierSavings(),
            calculationDate,
            savingContributions,
            savingContributionDates,
            input.dischargeDate(),
            existingInvestmentPrincipal,
            investmentContributions,
            request.getExpectedReturnRate());

    return new SimulationCalculationResult(
        asset,
        financialDischargeDate,
        totalMonths,
        input.baseAsset(),
        expectedSalary,
        expectedSpending,
        benefit.soldierSavingPrincipal(),
        benefit.soldierSavingInterest(),
        benefit.governmentMatchingSupport(),
        benefit.investmentPrincipal(),
        benefit.expectedInvestmentReturn(),
        unallocatedPrincipal,
        CALCULATION_POLICY_VERSION);
  }

  private long scale(BigDecimal ratio, long referenceSalary) {
    return ratio.multiply(BigDecimal.valueOf(referenceSalary))
        .setScale(0, java.math.RoundingMode.HALF_UP)
        .longValueExact();
  }
```

Note this drops the old `request.getMonthlySpendingAmount()`/`request.getMonthlyInvestmentAmount()` flat reuse per month — every month now goes through `scale(...)`. `unallocatedPrincipal`'s formula is unchanged in shape, just fed the scaled `spending`/`investment` instead of the flat request values.

Also bump `CALCULATION_POLICY_VERSION`:

```java
  public static final String CALCULATION_POLICY_VERSION = "WHAT_IF_DETAIL_V3_20260813";
```

- [ ] **Step 8: Update `emptyResult`**

Drop the trailing `potentialExpectedAsset` argument (currently duplicating `input.baseAsset()` as the 14th constructor argument before `CALCULATION_POLICY_VERSION`) to match `SimulationCalculationResult`'s new 13-field shape from Step 4.

- [ ] **Step 9: Run tests to verify they pass**

Run: `./gradlew test --tests "com.jaedaero.domain.simulation.service.*"`
Expected: PASS

- [ ] **Step 10: Commit**

```bash
git add src/main/java/com/jaedaero/domain/simulation/service/SimulationCalculator.java \
        src/main/java/com/jaedaero/domain/simulation/service/SimulationInput.java \
        src/main/java/com/jaedaero/domain/simulation/service/SimulationCalculationResult.java \
        src/main/java/com/jaedaero/domain/simulation/service/CashflowSimulationInputProvider.java \
        src/main/java/com/jaedaero/domain/cashflow/dto/CashflowCalculationInputResponse.java \
        src/test/java/com/jaedaero/domain/simulation/service/
git commit -m "feat: scale simulation spending/investment with salary, wire investment principal into simulation calculator"
```

---

### Task 9: `SimulationVo`/`SimulationExpectedEffectResponse`/`SimulationServiceImpl`/mapper — drop dual asset fields

**Files:**
- Modify: `src/main/java/com/jaedaero/domain/simulation/vo/SimulationVo.java`
- Modify: `src/main/java/com/jaedaero/domain/simulation/dto/SimulationExpectedEffectResponse.java`
- Modify: `src/main/java/com/jaedaero/domain/simulation/service/impl/SimulationServiceImpl.java`
- Modify: `src/main/resources/mapper/simulation/SimulationMapper.xml`
- Test: `src/test/java/com/jaedaero/domain/simulation/service/SimulationServiceImplTest.java`

**Interfaces:**
- Consumes: `SimulationCalculationResult` (Task 8's new shape)
- Produces: `SimulationExpectedEffectResponse` without `conservativeExpectedAsset`/`potentialExpectedAsset`/`returnsIncludedInExpectedAsset`.

- [ ] **Step 1: Update the failing test**

In `SimulationServiceImplTest.java`, find every assertion referencing `getPotentialExpectedAsset()`/`getConservativeExpectedAsset()`/`getReturnsIncludedInExpectedAsset()` (on either `SimulationVo` or `SimulationExpectedEffectResponse`) and delete them. The single `expectedAsset` assertions on `SimulationResponse` stay unchanged (that field was never part of the dual-value pair — see Task 8's investigation notes).

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.jaedaero.domain.simulation.service.SimulationServiceImplTest"`
Expected: FAIL — compile errors on the deleted-but-still-referenced accessors (confirms you found them all; if it fails on missing new fields, keep going, you'll add them in Step 3-5).

- [ ] **Step 3: Update `SimulationVo`**

Remove `private Long potentialExpectedAsset;` from the field list. Nothing else in the class changes.

- [ ] **Step 4: Update `SimulationExpectedEffectResponse`**

```java
package com.jaedaero.domain.simulation.dto;

import com.jaedaero.domain.simulation.service.SimulationCalculator;
import com.jaedaero.domain.simulation.vo.SimulationVo;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

/** 군적금·투자 이자·수익 참고 정보를 원금과 분리해 제공한다. */
@Getter
@Builder
public class SimulationExpectedEffectResponse {

  private final BigDecimal soldierSavingAnnualInterestRate;
  private final BigDecimal governmentMatchingRate;
  private final BigDecimal investmentAnnualReturnRate;
  private final String compoundingPeriod;
  private final Long soldierSavingInterest;
  private final Long governmentMatchingSupport;
  private final Long expectedInvestmentReturn;
  private final Long projectedBenefitAmount;
  private final String calculationPolicyVersion;

  public static SimulationExpectedEffectResponse from(SimulationVo simulation) {
    if (simulation.getCalculationMonths() == null) {
      return null;
    }
    long projectedBenefit =
        Math.addExact(
            Math.addExact(
                simulation.getSoldierSavingInterest(), simulation.getGovernmentMatchingSupport()),
            simulation.getExpectedInvestmentReturn());
    return builder()
        .soldierSavingAnnualInterestRate(SimulationCalculator.SOLDIER_SAVING_ANNUAL_INTEREST_RATE)
        .governmentMatchingRate(SimulationCalculator.GOVERNMENT_MATCHING_RATE)
        .investmentAnnualReturnRate(simulation.getExpectedReturnRate())
        .compoundingPeriod("MONTHLY_END_OF_PERIOD")
        .soldierSavingInterest(simulation.getSoldierSavingInterest())
        .governmentMatchingSupport(simulation.getGovernmentMatchingSupport())
        .expectedInvestmentReturn(simulation.getExpectedInvestmentReturn())
        .projectedBenefitAmount(projectedBenefit)
        .calculationPolicyVersion(simulation.getCalculationPolicyVersion())
        .build();
  }
}
```

- [ ] **Step 5: Update `SimulationServiceImpl.run`**

In the `SimulationVo.builder()` chain (lines 108-132), delete the `.potentialExpectedAsset(result.potentialExpectedAsset())` line. Everything else stays.

- [ ] **Step 6: Update `SimulationMapper.xml`**

Remove `potential_expected_asset` from the `simulationColumns` SQL fragment (line 14), the `insert`'s column list (line 26) and its `VALUES` placeholder list (line 33).

- [ ] **Step 7: Run tests to verify they pass**

Run: `./gradlew test --tests "com.jaedaero.domain.simulation.*"`
Expected: PASS

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/jaedaero/domain/simulation/vo/SimulationVo.java \
        src/main/java/com/jaedaero/domain/simulation/dto/SimulationExpectedEffectResponse.java \
        src/main/java/com/jaedaero/domain/simulation/service/impl/SimulationServiceImpl.java \
        src/main/resources/mapper/simulation/SimulationMapper.xml \
        src/test/java/com/jaedaero/domain/simulation/service/SimulationServiceImplTest.java
git commit -m "feat: drop potential_expected_asset from simulation response"
```

---

### Task 10: Full-suite verification and stray-reference sweep

**Files:**
- Verify only — no new files expected, but grep may surface stray call sites Tasks 1-9 missed (Spring context wiring for the two new provider beans, any controller/other-domain code still calling `getPotentialExpectedAsset()`/`getReturnsIncludedInExpectedAsset()`/the old `calculateProjectedBenefit(3 args)`/the old `SimulationInput`/`CashflowInput` constructors without `userId`).

- [ ] **Step 1: Grep for anything still referencing the removed fields/signatures**

```bash
grep -rn "potentialExpectedAsset\|PotentialExpectedAsset\|potential_expected_asset\|returnsIncludedInExpectedAsset\|ReturnsIncludedInExpectedAsset" src/main src/test
grep -rn "new CashflowInput(" src/main src/test
grep -rn "new SimulationInput(" src/main src/test
grep -rn "calculateProjectedBenefit(" src/main src/test
```

Fix every remaining hit — likely candidates: `InvestmentGuidanceServiceImpl` or `InvestmentGuidanceCalculator` if they ever read `cashflow_forecast.potential_expected_asset`-backed fields (check with `grep -rn "potentialExpectedAsset" src/main/java/com/jaedaero/domain/investmentguidance`), any AI-coach/dashboard controller DTOs that pass through `CashflowForecastResponse.getPotentialExpectedAsset()`, and Spring bean qualifiers (`@Qualifier("mockDbBrokeragePositionProvider")`-style strings in Task 3's `EnvironmentAwareAggregateInvestmentPrincipalProvider` must exactly match the `@Component`-derived bean names of `LocalAggregateInvestmentPrincipalProvider`/`CodefAggregateInvestmentPrincipalProvider` — Spring defaults to the decapitalized class name, so double check against what actually gets registered by running the app or a `@SpringBootTest` context-load test).

- [ ] **Step 2: Run the full test suite**

Run: `./gradlew test`
Expected: PASS (or a clean, understood list of pre-existing unrelated failures — do not proceed if anything in `cashflow`, `simulation`, or `investmentguidance` packages fails)

- [ ] **Step 3: Confirm the Spring context loads**

Run: `./gradlew test --tests "*ApplicationContextTest*"` (or whatever this project's smoke test for full context load is named — grep `src/test` for `@SpringBootTest` with no narrower scope if unsure) — this catches the `AggregateInvestmentPrincipalProvider`'s two `@Qualifier` bean names actually resolving.

- [ ] **Step 4: Commit if Step 1 found and fixed anything**

```bash
git add -A
git commit -m "fix: sweep remaining references to removed potential-asset fields"
```
