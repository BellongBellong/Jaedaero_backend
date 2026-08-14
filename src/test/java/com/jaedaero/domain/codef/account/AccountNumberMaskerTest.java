package com.jaedaero.domain.codef.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class AccountNumberMaskerTest {

  @Test
  void masksRawAndPreviouslyMaskedAccountNumbers() {
    assertEquals("639602-**-****75", AccountNumberMasker.mask("639602-04-082475"));
    assertEquals("****-**-****75", AccountNumberMasker.mask("***-***-2475"));
    assertEquals("639602-**-****75", AccountNumberMasker.mask("639602-**-****75"));
    assertEquals("****-**-****", AccountNumberMasker.mask("1234"));
    assertNull(AccountNumberMasker.mask(null));
  }
}
