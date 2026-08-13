package com.jaedaero.domain.codef.token;

import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * CODEF OAuth 토큰을 만료 직전까지 재사용합니다.
 *
 * <p>토큰은 애플리케이션 메모리에만 보관하며 프런트엔드에 반환하거나 저장하지 않습니다.
 */
@Component
public class CodefAccessTokenProvider {

  private static final long REFRESH_BUFFER_SECONDS = 300L;

  private final CodefTokenClient codefTokenClient;
  private CachedToken cachedToken;

  public CodefAccessTokenProvider(CodefTokenClient codefTokenClient) {
    this.codefTokenClient = codefTokenClient;
  }

  public synchronized String getAccessToken() {
    if (cachedToken == null || cachedToken.expiresAt().isBefore(Instant.now())) {
      CodefTokenResponse tokenResponse = codefTokenClient.publishToken();
      long expiresIn = tokenResponse.getExpiresIn() == null ? 0L : tokenResponse.getExpiresIn();
      long cacheSeconds = Math.max(0L, expiresIn - REFRESH_BUFFER_SECONDS);
      cachedToken =
          new CachedToken(tokenResponse.getAccessToken(), Instant.now().plusSeconds(cacheSeconds));
    }

    return cachedToken.accessToken();
  }

  public synchronized void invalidate() {
    cachedToken = null;
  }

  private record CachedToken(String accessToken, Instant expiresAt) {}
}
