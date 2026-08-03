package com.jaedaero.domain.codef.connection;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;

/** CODEF가 지정한 비밀번호 필드를 CODEF RSA 공개키로 암호화합니다. */
public class CodefRsaEncryptor {

  public String encrypt(String plainText, String base64PublicKey) {
    if (base64PublicKey == null || base64PublicKey.isBlank()) {
      throw new IllegalStateException("CODEF RSA public key is not configured.");
    }

    try {
      byte[] publicKeyBytes = Base64.getDecoder().decode(base64PublicKey);
      PublicKey publicKey =
          KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBytes));

      Cipher cipher = Cipher.getInstance("RSA");
      cipher.init(Cipher.ENCRYPT_MODE, publicKey);
      byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(encrypted);
    } catch (Exception exception) {
      throw new IllegalArgumentException("CODEF password encryption failed.", exception);
    }
  }
}
