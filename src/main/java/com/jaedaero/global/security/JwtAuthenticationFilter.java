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

/** 운영 환경 JWT 필터입니다. 개발용 헤더 인증을 제거할 때 SecurityConfig에서 활성화합니다. */
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
