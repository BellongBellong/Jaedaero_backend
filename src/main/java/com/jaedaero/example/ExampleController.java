package com.jaedaero.example;

import java.util.Collections;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class ExampleController {

  @GetMapping
  public ResponseEntity<Map<String, String>> example() {
    return ResponseEntity.ok(Collections.singletonMap("status", "ok"));
  }
}
