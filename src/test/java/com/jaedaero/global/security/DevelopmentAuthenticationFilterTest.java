package com.jaedaero.global.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import javax.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class DevelopmentAuthenticationFilterTest {

  private final DevelopmentAuthenticationFilter filter =
      new DevelopmentAuthenticationFilter();

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void numericUserIdHeaderCreatesAuthentication() throws ServletException, IOException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-User-Id", "1");

    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    assertEquals("1", SecurityContextHolder.getContext().getAuthentication().getName());
  }

  @Test
  void invalidUserIdHeaderDoesNotCreateAuthentication() throws ServletException, IOException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-User-Id", "not-a-number");

    filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

    assertNull(SecurityContextHolder.getContext().getAuthentication());
  }
}
