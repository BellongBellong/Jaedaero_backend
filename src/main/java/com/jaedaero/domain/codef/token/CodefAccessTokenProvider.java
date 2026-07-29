package com.jaedaero.domain.codef.token;

import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Reuses the CODEF OAuth token until shortly before its expiration.
 *
 * <p>The token stays only in application memory. It is not returned to the frontend or persisted.
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
