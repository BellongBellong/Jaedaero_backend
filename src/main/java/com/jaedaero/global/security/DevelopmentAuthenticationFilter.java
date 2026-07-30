package com.jaedaero.global.security;

import java.io.IOException;
import java.util.List;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Development-only filter. It accepts X-User-Id solely to exercise authenticated endpoints locally. */
public class DevelopmentAuthenticationFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String userId = request.getHeader("X-User-Id");
    if (userId != null && userId.matches("\\d+") && SecurityContextHolder.getContext().getAuthentication() == null) {
      SecurityContextHolder.getContext()
          .setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }
    filterChain.doFilter(request, response);
  }
}
