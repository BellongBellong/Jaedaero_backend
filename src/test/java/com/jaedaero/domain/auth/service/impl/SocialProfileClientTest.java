package com.jaedaero.domain.auth.service.impl;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaedaero.domain.auth.common.enums.SocialType;
import com.jaedaero.domain.auth.vo.AuthUserVo;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class SocialProfileClientTest {

  @Test
  void exchangesGoogleAuthorizationCodeAndLoadsProfile() {
    RestTemplate restTemplate = new RestTemplate();
    MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
    SocialProfileClient client =
        new SocialProfileClient(
            restTemplate,
            new ObjectMapper(),
            "google-client-id",
            "google-client-secret",
            "kakao-client-id",
            "");

    server
        .expect(requestTo("https://oauth2.googleapis.com/token"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
        .andExpect(content().string(containsString("code=google-authorization-code")))
        .andExpect(content().string(containsString("client_id=google-client-id")))
        .andRespond(
            withSuccess("{\"access_token\":\"google-access-token\"}", MediaType.APPLICATION_JSON));
    server
        .expect(requestTo("https://openidconnect.googleapis.com/v1/userinfo"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("Authorization", "Bearer google-access-token"))
        .andRespond(withSuccess("{\"sub\":\"google-user-1\"}", MediaType.APPLICATION_JSON));

    AuthUserVo profile =
        client.exchangeAuthorizationCodeAndGetProfile(
            SocialType.GOOGLE,
            "google-authorization-code",
            "http://localhost:5173/auth/callback");

    assertEquals("google-user-1", profile.getSocialId());
    server.verify();
  }

  @Test
  void exchangesKakaoAuthorizationCodeAndLoadsProfile() {
    RestTemplate restTemplate = new RestTemplate();
    MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
    SocialProfileClient client =
        new SocialProfileClient(
            restTemplate,
            new ObjectMapper(),
            "google-client-id",
            "google-client-secret",
            "kakao-client-id",
            "");

    server
        .expect(requestTo("https://kauth.kakao.com/oauth/token"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
        .andExpect(content().string(containsString("code=kakao-authorization-code")))
        .andExpect(content().string(containsString("client_id=kakao-client-id")))
        .andRespond(
            withSuccess("{\"access_token\":\"kakao-access-token\"}", MediaType.APPLICATION_JSON));
    server
        .expect(requestTo("https://kapi.kakao.com/v2/user/me"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("Authorization", "Bearer kakao-access-token"))
        .andRespond(withSuccess("{\"id\":12345}", MediaType.APPLICATION_JSON));

    AuthUserVo profile =
        client.exchangeAuthorizationCodeAndGetProfile(
            SocialType.KAKAO,
            "kakao-authorization-code",
            "http://localhost:5173/auth/callback");

    assertEquals("12345", profile.getSocialId());
    server.verify();
  }
}
