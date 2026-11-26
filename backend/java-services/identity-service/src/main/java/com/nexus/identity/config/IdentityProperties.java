package com.nexus.identity.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "nexus.identity")
public record IdentityProperties(
        @NotBlank String issuer,
        @NotNull Duration accessTokenTtl,
        @NotNull Duration refreshTokenTtl,
        @Min(1) int maxFailedAttempts,
        @NotNull Duration lockDuration,
        @NotNull RateLimit rateLimit,
        @NotNull Jwt jwt,
        @NotNull Cors cors) {

    public record RateLimit(@Min(1) int requestsPerMinute) {}

    public record Jwt(@NotBlank String keyId, String privateKey, String publicKey) {}

    public record Cors(List<String> allowedOrigins) {}
}
