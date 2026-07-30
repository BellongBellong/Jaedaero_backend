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
import lombok.RequiredArgsConstructor;

/** Production JWT filter. Enable it in SecurityConfig when development header authentication is removed. */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider jwtTokenProvider;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String authorization = request.getHeader("Authorization");
    if (authorization != null
        && authorization.startsWith("Bearer ")
        && SecurityContextHolder.getContext().getAuthentication() == null) {
      Long userId = jwtTokenProvider.getUserId(authorization.substring(7));
      if (userId != null) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()));
      }
    }
    filterChain.doFilter(request, response);
  }
}
