package com.jaedaero.krx.etf;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Browser-only demonstration page for the public ETF lookup API. */
@Controller
@RequestMapping("/etf-demo")
public class KrxEtfDemoController {

    @GetMapping
    public String page(Model model) {
        model.addAttribute("defaultDate", LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
        return "etf-demo/index";
    }
}
