package com.zenius.one.modules.identity.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class TotpServiceTest {

    private final TotpService totpService = new TotpService();

    @Test
    void generatesAndVerifiesValidTotpCode() {
        String secret = totpService.generateSecret();
        Instant now = Instant.now();
        String code = totpService.generateCodeForTime(secret, now);

        assertThat(code).matches("\\d{6}");
        assertThat(totpService.verifyCode(secret, code, now)).isTrue();
        assertThat(totpService.verifyCode(secret, "000000", now.plusSeconds(300))).isFalse();
        assertThat(totpService.verifyCode(secret, "abc", now)).isFalse();
    }
}

