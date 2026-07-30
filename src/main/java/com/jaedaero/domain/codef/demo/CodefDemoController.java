package com.jaedaero.domain.codef.demo;

import com.jaedaero.domain.codef.institution.CodefBankInstitution;
import com.jaedaero.domain.codef.institution.CodefBusinessType;
import com.jaedaero.domain.codef.institution.CodefSecuritiesInstitution;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Server-rendered local demonstration of CODEF login, account cards, and transaction history. */
@Controller
@RequestMapping("/codef-demo")
public class CodefDemoController {

  private static final String ACTIVE_KEY = "codef.demo.active";
  private static final String ORGANIZATION_KEY = "codef.demo.organization";
  private static final String BUSINESS_TYPE_KEY = "codef.demo.business-type";
  private static final Logger LOGGER = Logger.getLogger(CodefDemoController.class.getName());

  private final CodefDemoService codefDemoService;
  private final boolean enabled;
  private final long demoUserId;

  public CodefDemoController(
      CodefDemoService codefDemoService,
      @Value("${codef.debug.account-create-enabled:true}") boolean enabled,
      @Value("${codef.demo.user-id:1}") long demoUserId) {
    this.codefDemoService = codefDemoService;
    this.enabled = enabled;
    this.demoUserId = demoUserId;
  }

  @GetMapping
  public String loginPage(Model model) {
    assertEnabled();
    addInstitutionCatalogues(model);
    model.addAttribute("savedInstitutions", codefDemoService.getSavedInstitutions(demoUserId));
    model.addAttribute("loginForm", new CodefDemoLoginForm());
    model.addAttribute("demoUserId", demoUserId);
    return "codef-demo/login";
  }

  @GetMapping("/accounts")
  public String accounts(
      @RequestParam(required = false) String organizationCode,
      @RequestParam(required = false) String businessType,
      @RequestParam(defaultValue = "false") boolean refresh,
      HttpSession session,
      Model model) {
    assertEnabled();
    if (organizationCode != null && businessType != null) {
      institutionDisplayName(organizationCode, businessType);
      session.setAttribute(ACTIVE_KEY, Boolean.TRUE);
      session.setAttribute(ORGANIZATION_KEY, organizationCode);
      session.setAttribute(BUSINESS_TYPE_KEY, businessType);
    }
    organizationCode = getSessionValue(session, ORGANIZATION_KEY);
    businessType = getSessionValue(session, BUSINESS_TYPE_KEY);
    if (!isActive(session) || organizationCode == null || businessType == null) {
      return "redirect:/codef-demo";
    }
    model.addAttribute("demoUserId", demoUserId);
    model.addAttribute("organizationCode", organizationCode);
    model.addAttribute("businessType", businessType);
    model.addAttribute("refresh", refresh);
    model.addAttribute("institutionDisplayName", institutionDisplayName(organizationCode, businessType));
    return "codef-demo/accounts-api";
  }

  @PostMapping("/transactions")
  public String transactions(
      @RequestParam("accountId") long accountId,
      @RequestParam(value = "transactionKind", defaultValue = "DEMAND_DEPOSIT")
          String transactionKind,
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate,
      HttpSession session,
      Model model,
      RedirectAttributes redirectAttributes) {
    assertEnabled();
    String organizationCode = getSessionValue(session, ORGANIZATION_KEY);
    String businessType = getSessionValue(session, BUSINESS_TYPE_KEY);
    if (!isActive(session) || organizationCode == null || businessType == null) {
      return "redirect:/codef-demo";
    }

    try {
      LocalDate queryEndDate = parseInputDateOrDefault(endDate, LocalDate.now());
      LocalDate queryStartDate = parseInputDateOrDefault(startDate, queryEndDate.minusMonths(3));
      if (queryStartDate.isAfter(queryEndDate)) {
        throw new IllegalArgumentException("시작일은 종료일보다 늦을 수 없습니다.");
      }
      model.addAttribute(
          "transactions",
          codefDemoService.getRecentTransactions(
              demoUserId, accountId, transactionKind, queryStartDate, queryEndDate));
      model.addAttribute("accountId", accountId);
      model.addAttribute("accountDisplay", findAccountDisplay(accountId));
      model.addAttribute("transactionKind", transactionKind);
      model.addAttribute("startDate", queryStartDate);
      model.addAttribute("endDate", queryEndDate);
      model.addAttribute(
          "institutionDisplayName", institutionDisplayName(organizationCode, businessType));
      return "codef-demo/transactions";
    } catch (RuntimeException exception) {
      LOGGER.log(Level.WARNING, "CODEF demo transaction lookup failed", exception);
      redirectAttributes.addFlashAttribute(
          "errorMessage", "거래내역을 불러오지 못했습니다. 서버 로그에서 상세 원인을 확인해주세요.");
      return "redirect:/codef-demo/accounts";
    }
  }

  @PostMapping("/all-assets")
  public String allAssets(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
    assertEnabled();
    if (!isActive(session)) {
      return "redirect:/codef-demo";
    }
    try {
      model.addAttribute("unifiedAssets", codefDemoService.getUnifiedAssets(demoUserId));
      return "codef-demo/all-assets";
    } catch (RuntimeException exception) {
      LOGGER.log(Level.WARNING, "CODEF demo unified asset lookup failed", exception);
      redirectAttributes.addFlashAttribute(
          "errorMessage", "전체 자산을 불러오지 못했습니다. 서버 로그에서 상세 원인을 확인해주세요.");
      return "redirect:/codef-demo/accounts";
    }
  }

  @GetMapping("/securities/assets")
  public String securitiesAssets(
      @RequestParam long accountId,
      HttpSession session,
      Model model,
      RedirectAttributes redirectAttributes) {
    return renderSecuritiesPortfolio(accountId, session, model, redirectAttributes, false);
  }

  @GetMapping("/securities/holdings")
  public String securitiesHoldings(
      @RequestParam long accountId,
      HttpSession session,
      Model model,
      RedirectAttributes redirectAttributes) {
    return renderSecuritiesPortfolio(accountId, session, model, redirectAttributes, true);
  }

  @PostMapping("/disconnect")
  public String disconnect(HttpSession session) {
    session.removeAttribute(ACTIVE_KEY);
    session.removeAttribute(ORGANIZATION_KEY);
    session.removeAttribute(BUSINESS_TYPE_KEY);
    return "redirect:/codef-demo";
  }

  private LocalDate parseInputDateOrDefault(String value, LocalDate defaultValue) {
    if (value == null || value.isBlank()) {
      return defaultValue;
    }
    try {
      return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
    } catch (RuntimeException exception) {
      throw new IllegalArgumentException("조회 날짜가 올바르지 않습니다.");
    }
  }

  private void assertEnabled() {
    if (!enabled) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.NOT_FOUND);
    }
  }

  private String getSessionValue(HttpSession session, String key) {
    Object value = session.getAttribute(key);
    return value instanceof String ? (String) value : null;
  }

  private boolean isActive(HttpSession session) {
    return Boolean.TRUE.equals(session.getAttribute(ACTIVE_KEY));
  }

  private String findAccountDisplay(long accountId) {
    // The service verifies ownership before rendering. This avoids placing a real account number in
    // the session.
    return codefDemoService.getAccountDisplay(demoUserId, accountId);
  }

  private String renderSecuritiesPortfolio(
      long accountId,
      HttpSession session,
      Model model,
      RedirectAttributes redirectAttributes,
      boolean stockOnly) {
    assertEnabled();
    String organizationCode = getSessionValue(session, ORGANIZATION_KEY);
    String businessType = getSessionValue(session, BUSINESS_TYPE_KEY);
    if (!isActive(session)
        || organizationCode == null
        || !CodefBusinessType.SECURITIES.getCode().equals(businessType)) {
      return "redirect:/codef-demo";
    }
    try {
      model.addAttribute(
          "portfolio",
          stockOnly
              ? codefDemoService.getStockHoldings(demoUserId, accountId)
              : codefDemoService.getSecuritiesAssets(demoUserId, accountId));
      model.addAttribute("stockOnly", stockOnly);
      model.addAttribute(
          "institutionDisplayName", institutionDisplayName(organizationCode, businessType));
      return "codef-demo/securities-portfolio";
    } catch (RuntimeException exception) {
      LOGGER.log(Level.WARNING, "CODEF demo securities lookup failed", exception);
      redirectAttributes.addFlashAttribute(
          "errorMessage", "증권 정보를 불러오지 못했습니다. CODEF 응답과 서버 로그를 확인해주세요.");
      return "redirect:/codef-demo/accounts";
    }
  }

  private String institutionDisplayName(String organizationCode, String businessType) {
    if (CodefBusinessType.BANK.getCode().equals(businessType)) {
      return CodefBankInstitution.fromOrganizationCode(organizationCode).getDisplayName();
    }
    return CodefSecuritiesInstitution.fromOrganizationCode(organizationCode).getDisplayName();
  }

  private void addInstitutionCatalogues(Model model) {
    model.addAttribute("banks", Arrays.asList(CodefBankInstitution.values()));
    model.addAttribute("securities", Arrays.asList(CodefSecuritiesInstitution.values()));
  }
}
