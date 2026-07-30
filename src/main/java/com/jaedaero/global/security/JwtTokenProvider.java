package com.jaedaero.global.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

  private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

  private final ObjectMapper objectMapper;
  private final byte[] secret;
  private final long accessTokenExpirationSeconds;
  private final long refreshTokenExpirationSeconds;
  private final SecureRandom secureRandom = new SecureRandom();

  public JwtTokenProvider(
      @Value("${JWT_SECRET:}") String base64Secret,
      @Value("${JWT_ACCESS_TOKEN_EXPIRATION_SECONDS:3600}") long accessTokenExpirationSeconds,
      @Value("${JWT_REFRESH_TOKEN_EXPIRATION_SECONDS:1209600}") long refreshTokenExpirationSeconds,
      Dotenv dotenv,
      ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    String dotenvSecret = dotenv.get("JWT_SECRET");
    this.secret = decodeSecret(dotenvSecret == null ? base64Secret : dotenvSecret);
    if (accessTokenExpirationSeconds <= 0) {
      throw new IllegalStateException("JWT_ACCESS_TOKEN_EXPIRATION_SECONDS must be positive.");
    }
    this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
    if (refreshTokenExpirationSeconds <= 0) {
      throw new IllegalStateException("JWT_REFRESH_TOKEN_EXPIRATION_SECONDS must be positive.");
    }
    this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
  }

  public String createAccessToken(long userId) {
    try {
      Instant now = Instant.now();
      String header = encodeJson("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
      String payload =
          encodeJson(
              objectMapper.writeValueAsString(
                  java.util.Map.of(
                      "sub", String.valueOf(userId),
                      "iat", now.getEpochSecond(),
                      "exp", now.plusSeconds(accessTokenExpirationSeconds).getEpochSecond())));
      String unsignedToken = header + "." + payload;
      return unsignedToken + "." + URL_ENCODER.encodeToString(sign(unsignedToken));
    } catch (Exception exception) {
      throw new IllegalStateException("JWT access token could not be created.", exception);
    }
  }

  public Long getUserId(String token) {
    try {
      String[] parts = token.split("\\.");
      if (parts.length != 3) {
        return null;
      }
      byte[] expectedSignature = sign(parts[0] + "." + parts[1]);
      byte[] actualSignature = URL_DECODER.decode(parts[2]);
      if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
        return null;
      }
      JsonNode payload = objectMapper.readTree(URL_DECODER.decode(parts[1]));
      if (payload.path("exp").asLong(0) <= Instant.now().getEpochSecond()) {
        return null;
      }
      return Long.parseLong(payload.path("sub").asText());
    } catch (Exception exception) {
      return null;
    }
  }

  public String createRefreshToken() {
    byte[] bytes = new byte[48];
    secureRandom.nextBytes(bytes);
    return URL_ENCODER.encodeToString(bytes);
  }

  public long getAccessTokenExpirationSeconds() {
    return accessTokenExpirationSeconds;
  }

  public long getRefreshTokenExpirationSeconds() {
    return refreshTokenExpirationSeconds;
  }

  private byte[] decodeSecret(String base64Secret) {
    try {
      byte[] decoded = Base64.getDecoder().decode(base64Secret);
      if (decoded.length < 32) {
        throw new IllegalStateException(
            "JWT_SECRET must be a Base64-encoded secret of at least 32 bytes.");
      }
      return decoded;
    } catch (IllegalArgumentException exception) {
      throw new IllegalStateException("JWT_SECRET must be Base64 encoded.", exception);
    }
  }

  private String encodeJson(String json) {
    return URL_ENCODER.encodeToString(json.getBytes(StandardCharsets.UTF_8));
  }

  private byte[] sign(String value) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret, "HmacSHA256"));
    return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
  }
}
