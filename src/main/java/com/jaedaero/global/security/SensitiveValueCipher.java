package com.jaedaero.global.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Encrypts Connected IDs and account numbers before they are persisted. */
@Component
public class SensitiveValueCipher {

  private static final String TRANSFORMATION = "AES/GCM/NoPadding";
  private static final int IV_LENGTH = 12;
  private static final int TAG_LENGTH_BITS = 128;

  private final SecretKey key;
  private final SecureRandom secureRandom = new SecureRandom();

  public SensitiveValueCipher(@Value("${app.crypto.master-key-base64}") String base64Key) {
    byte[] keyBytes;
    try {
      keyBytes = Base64.getDecoder().decode(base64Key);
    } catch (IllegalArgumentException exception) {
      throw new IllegalStateException(
          "app.crypto.master-key-base64 must be Base64 encoded.", exception);
    }
    if (keyBytes.length != 32) {
      throw new IllegalStateException(
          "app.crypto.master-key-base64 must decode to a 32-byte AES-256 key.");
    }
    this.key = new SecretKeySpec(keyBytes, "AES");
  }

  public String encrypt(String plainText) {
    try {
      byte[] iv = new byte[IV_LENGTH];
      secureRandom.nextBytes(iv);
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
      byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
      ByteBuffer payload = ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted);
      return Base64.getEncoder().encodeToString(payload.array());
    } catch (Exception exception) {
      throw new IllegalStateException("Sensitive value encryption failed.", exception);
    }
  }

  public String decrypt(String encryptedValue) {
    try {
      byte[] payload = Base64.getDecoder().decode(encryptedValue);
      ByteBuffer buffer = ByteBuffer.wrap(payload);
      byte[] iv = new byte[IV_LENGTH];
      buffer.get(iv);
      byte[] encrypted = new byte[buffer.remaining()];
      buffer.get(encrypted);
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
      return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    } catch (Exception exception) {
      throw new IllegalStateException("Sensitive value decryption failed.", exception);
    }
  }
}
