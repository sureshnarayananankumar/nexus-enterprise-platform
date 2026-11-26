package com.nexus.identity;

import com.nexus.identity.application.TokenHasher;
import com.nexus.identity.domain.UserAccount;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserAccountTest {
    private final Instant now = Instant.parse("2026-11-26T10:00:00Z");

    @Test
    void locksAfterMaxAttemptsAndUnlocksAfterDuration() {
        UserAccount u = UserAccount.create(UUID.randomUUID(), " Admin@Acme.TEST ", "{noop}x", "A", List.of(), now);
        assertThat(u.getEmail()).isEqualTo("Admin@Acme.TEST");

        for (int i = 0; i < 4; i++) assertThat(u.registerFailedLogin(5, Duration.ofMinutes(15), now)).isFalse();
        assertThat(u.registerFailedLogin(5, Duration.ofMinutes(15), now)).isTrue();
        assertThat(u.isLocked(now.plusSeconds(60))).isTrue();
        assertThat(u.isLocked(now.plus(Duration.ofMinutes(16)))).isFalse();

        u.registerSuccessfulLogin(now.plus(Duration.ofMinutes(16)));
        assertThat(u.getFailedLoginAttempts()).isZero();
        assertThat(u.getLockedUntil()).isNull();
    }

    @Test
    void emailNormalisationIsCaseAndWhitespaceInsensitive() {
        assertThat(UserAccount.normalizeEmail("  Admin@Acme.TEST ")).isEqualTo("admin@acme.test");
    }

    @Test
    void opaqueTokensAreRandomAndHashedDeterministically() {
        String a = TokenHasher.newOpaqueToken();
        String b = TokenHasher.newOpaqueToken();
        assertThat(a).hasSize(43).isNotEqualTo(b);
        assertThat(TokenHasher.sha256Hex(a)).hasSize(64).isEqualTo(TokenHasher.sha256Hex(a));
    }
}
