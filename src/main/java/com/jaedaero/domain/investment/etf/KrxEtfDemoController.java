package com.jaedaero.domain.investment.etf;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** 공개 ETF 조회 API를 위한 브라우저 전용 데모 페이지입니다. */
@Controller
@RequestMapping("/etf-demo")
public class KrxEtfDemoController {

  @GetMapping
  public String page(Model model) {
    model.addAttribute("defaultDate", LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
    return "etf-demo/index";
  }
}
