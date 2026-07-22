package com.jaedaero.codef.demo;

import com.jaedaero.codef.institution.CodefBankInstitution;
import java.util.Arrays;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Server-rendered local demonstration of CODEF login, account cards, and transaction history. */
@Controller
@RequestMapping("/codef-demo")
public class CodefDemoController {

    private static final String CONNECTED_ID_KEY = "codef.demo.connected-id";
    private static final String ORGANIZATION_KEY = "codef.demo.organization";

    private final CodefDemoService codefDemoService;
    private final boolean enabled;

    public CodefDemoController(
            CodefDemoService codefDemoService,
            @Value("${codef.debug.account-create-enabled:false}") boolean enabled) {
        this.codefDemoService = codefDemoService;
        this.enabled = enabled;
    }

    @GetMapping
    public String loginPage(Model model) {
        assertEnabled();
        model.addAttribute("banks", Arrays.asList(CodefBankInstitution.values()));
        model.addAttribute("loginForm", new CodefDemoLoginForm());
        return "codef-demo/login";
    }

    @PostMapping("/connect")
    public String connect(
            @Valid @ModelAttribute("loginForm") CodefDemoLoginForm loginForm,
            BindingResult bindingResult,
            HttpSession session,
            Model model) {
        assertEnabled();
        if (bindingResult.hasErrors()) {
            model.addAttribute("banks", Arrays.asList(CodefBankInstitution.values()));
            return "codef-demo/login";
        }

        try {
            String connectedId = codefDemoService.connect(loginForm);
            session.setAttribute(CONNECTED_ID_KEY, connectedId);
            session.setAttribute(ORGANIZATION_KEY, loginForm.getOrganizationCode());
            return accounts(session, model);
        } catch (RuntimeException exception) {
            model.addAttribute("banks", Arrays.asList(CodefBankInstitution.values()));
            model.addAttribute("errorMessage", "은행 연결에 실패했습니다. 입력한 인증 정보와 CODEF 기관 지원 방식을 확인해주세요.");
            return "codef-demo/login";
        }
    }

    @GetMapping("/accounts")
    public String accounts(HttpSession session, Model model) {
        assertEnabled();
        String connectedId = getSessionValue(session, CONNECTED_ID_KEY);
        String organizationCode = getSessionValue(session, ORGANIZATION_KEY);
        if (connectedId == null || organizationCode == null) {
            return "redirect:/codef-demo";
        }

        try {
            model.addAttribute("accounts", codefDemoService.getAccountCards(connectedId, organizationCode));
            model.addAttribute("bank", CodefBankInstitution.fromOrganizationCode(organizationCode));
            return "codef-demo/accounts";
        } catch (RuntimeException exception) {
            session.removeAttribute(CONNECTED_ID_KEY);
            session.removeAttribute(ORGANIZATION_KEY);
            return "redirect:/codef-demo";
        }
    }

    @PostMapping("/transactions")
    public String transactions(
            @RequestParam("account") String account, HttpSession session, Model model) {
        assertEnabled();
        String connectedId = getSessionValue(session, CONNECTED_ID_KEY);
        String organizationCode = getSessionValue(session, ORGANIZATION_KEY);
        if (connectedId == null || organizationCode == null) {
            return "redirect:/codef-demo";
        }

        try {
            model.addAttribute(
                    "transactions", codefDemoService.getRecentTransactions(connectedId, organizationCode, account));
            model.addAttribute("account", account);
            model.addAttribute("bank", CodefBankInstitution.fromOrganizationCode(organizationCode));
            return "codef-demo/transactions";
        } catch (RuntimeException exception) {
            return "redirect:/codef-demo/accounts";
        }
    }

    @PostMapping("/disconnect")
    public String disconnect(HttpSession session) {
        session.removeAttribute(CONNECTED_ID_KEY);
        session.removeAttribute(ORGANIZATION_KEY);
        return "redirect:/codef-demo";
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
}
