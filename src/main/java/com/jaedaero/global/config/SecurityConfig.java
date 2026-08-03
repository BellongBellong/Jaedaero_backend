package com.jaedaero.global.config;

import com.jaedaero.global.security.DevelopmentAuthenticationFilter;
import com.jaedaero.global.security.JwtAccessDeniedHandler;
import com.jaedaero.global.security.JwtAuthenticationFilter;
import com.jaedaero.global.security.JwtAuthenticationEntryPoint;
import com.jaedaero.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@SuppressWarnings("deprecation")
@RequiredArgsConstructor
public class SecurityConfig extends WebSecurityConfigurerAdapter {

  private final JwtTokenProvider jwtTokenProvider;
  private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
  private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

  @Value("${app.environment:production}")
  private String appEnvironment;

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.csrf().disable()
        .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and()
        .exceptionHandling()
        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
        .accessDeniedHandler(jwtAccessDeniedHandler)
        .and()
        .authorizeRequests()
        .antMatchers("/api/v1/auth/login", "/api/v1/auth/refresh", "/swagger-ui.html", "/swagger-ui/**", "/swagger-resources/**", "/v2/api-docs", "/webjars/**")
        .permitAll()
        .anyRequest()
        .permitAll();

    if ("local".equalsIgnoreCase(appEnvironment)) {
      http.addFilterBefore(
          new DevelopmentAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
      http.addFilterBefore(
          new JwtAuthenticationFilter(jwtTokenProvider), DevelopmentAuthenticationFilter.class);
    } else {
      http.addFilterBefore(
          new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);
    }
  }
}
