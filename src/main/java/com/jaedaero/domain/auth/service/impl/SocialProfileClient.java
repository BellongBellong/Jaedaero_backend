package com.jaedaero.domain.auth.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaedaero.domain.auth.common.enums.SocialType;
import com.jaedaero.domain.auth.exception.AuthErrorCode;
import com.jaedaero.domain.auth.exception.SocialAuthenticationException;
import com.jaedaero.domain.auth.vo.AuthUserVo;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class SocialProfileClient {

  private static final URI GOOGLE_USER_INFO_URI =
      URI.create("https://openidconnect.googleapis.com/v1/userinfo");
  private static final URI KAKAO_USER_INFO_URI = URI.create("https://kapi.kakao.com/v2/user/me");
  private static final URI GOOGLE_TOKEN_URI = URI.create("https://oauth2.googleapis.com/token");
  private static final URI KAKAO_TOKEN_URI = URI.create("https://kauth.kakao.com/oauth/token");

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;
  private final String googleClientId;
  private final String googleClientSecret;
  private final String kakaoClientId;
  private final String kakaoClientSecret;

  public SocialProfileClient(
      RestTemplate restTemplate,
      ObjectMapper objectMapper,
      @Value("${GOOGLE_CLIENT_ID:}") String googleClientId,
      @Value("${GOOGLE_CLIENT_SECRET:}") String googleClientSecret,
      @Value("${KAKAO_CLIENT_ID:}") String kakaoClientId,
      @Value("${KAKAO_CLIENT_SECRET:}") String kakaoClientSecret) {
    this.restTemplate = restTemplate;
    this.objectMapper = objectMapper;
    this.googleClientId = googleClientId;
    this.googleClientSecret = googleClientSecret;
    this.kakaoClientId = kakaoClientId;
    this.kakaoClientSecret = kakaoClientSecret;
  }

  public AuthUserVo exchangeAuthorizationCodeAndGetProfile(
      SocialType socialType, String authorizationCode, String redirectUri) {
    String accessToken = exchangeAuthorizationCode(socialType, authorizationCode, redirectUri);
    URI endpoint = socialType == SocialType.GOOGLE ? GOOGLE_USER_INFO_URI : KAKAO_USER_INFO_URI;
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(accessToken);
      ResponseEntity<String> response =
          restTemplate.exchange(
              endpoint, HttpMethod.GET, new HttpEntity<>(headers), String.class);
      return socialType == SocialType.GOOGLE
          ? parseGoogleProfile(response.getBody())
          : parseKakaoProfile(response.getBody());
    } catch (SocialAuthenticationException exception) {
      throw exception;
    } catch (HttpStatusCodeException exception) {
      throw new SocialAuthenticationException(
          AuthErrorCode.SOCIAL_AUTHENTICATION_FAILED, "Social token validation failed.", exception);
    } catch (RestClientException exception) {
      throw new SocialAuthenticationException(
          AuthErrorCode.SOCIAL_PROVIDER_UNAVAILABLE,
          "Social login provider is unavailable.",
          exception);
    } catch (Exception exception) {
      throw new SocialAuthenticationException(
          AuthErrorCode.SOCIAL_PROVIDER_UNAVAILABLE,
          "Social login provider is unavailable.",
          exception);
    }
  }

  private String exchangeAuthorizationCode(
      SocialType socialType, String authorizationCode, String redirectUri) {
    try {
      MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
      form.add("grant_type", "authorization_code");
      form.add("code", authorizationCode);
      form.add("redirect_uri", redirectUri);
      URI tokenUri;
      if (socialType == SocialType.GOOGLE) {
        requireClientCredentials(googleClientId, googleClientSecret);
        form.add("client_id", googleClientId);
        form.add("client_secret", googleClientSecret);
        tokenUri = GOOGLE_TOKEN_URI;
      } else {
        requireClientCredentials(kakaoClientId, null);
        form.add("client_id", kakaoClientId);
        if (kakaoClientSecret != null && !kakaoClientSecret.isBlank()) {
          form.add("client_secret", kakaoClientSecret);
        }
        tokenUri = KAKAO_TOKEN_URI;
      }
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
      ResponseEntity<String> response =
          restTemplate.postForEntity(tokenUri, new HttpEntity<>(form, headers), String.class);
      return requiredText(objectMapper.readTree(response.getBody()), "access_token");
    } catch (SocialAuthenticationException exception) {
      throw exception;
    } catch (HttpStatusCodeException exception) {
      throw new SocialAuthenticationException(
          AuthErrorCode.SOCIAL_AUTHENTICATION_FAILED,
          "Social authorization code validation failed.",
          exception);
    } catch (Exception exception) {
      throw new SocialAuthenticationException(
          AuthErrorCode.SOCIAL_PROVIDER_UNAVAILABLE,
          "Social login provider is unavailable.",
          exception);
    }
  }

  private void requireClientCredentials(String clientId, String clientSecret) {
    if (clientId == null || clientId.isBlank() || (clientSecret != null && clientSecret.isBlank())) {
      throw new SocialAuthenticationException(
          AuthErrorCode.SOCIAL_PROVIDER_UNAVAILABLE, "Social client credentials are not configured.");
    }
  }

  private AuthUserVo parseGoogleProfile(String responseBody) throws Exception {
    JsonNode root = objectMapper.readTree(responseBody);
    return socialProfile(requiredText(root, "sub"));
  }

  private AuthUserVo parseKakaoProfile(String responseBody) throws Exception {
    JsonNode root = objectMapper.readTree(responseBody);
    return socialProfile(requiredText(root, "id"));
  }

  private AuthUserVo socialProfile(String socialId) {
    AuthUserVo user = new AuthUserVo();
    user.setSocialId(socialId);
    return user;
  }

  private String requiredText(JsonNode node, String fieldName) {
    String value = optionalText(node, fieldName);
    if (value == null) {
      throw new SocialAuthenticationException(
          AuthErrorCode.SOCIAL_AUTHENTICATION_FAILED, "Social profile has no identifier.");
    }
    return value;
  }

  private String optionalText(JsonNode node, String fieldName) {
    String value = node.path(fieldName).asText(null);
    return value == null || value.isBlank() ? null : value;
  }
}
