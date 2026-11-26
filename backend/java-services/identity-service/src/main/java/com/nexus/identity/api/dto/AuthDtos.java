package com.nexus.identity.api.dto;

import com.nexus.identity.application.TokenPair;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class AuthDtos {
    /** 12-128 chars, at least one letter and one digit. */
    public static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d).{12,128}$";
    public static final String PASSWORD_MESSAGE = "must be 12-128 characters and contain a letter and a digit";

    private AuthDtos() {}

    public record LoginRequest(
            @NotBlank @Size(max = 63) @Schema(example = "acme") String tenant,
            @NotBlank @Email @Size(max = 254) @Schema(example = "admin@acme.test") String email,
            @NotBlank @Size(max = 128) String password) {}

    public record RefreshRequest(@NotBlank @Size(max = 128) String refreshToken) {}

    public record TokenResponse(String tokenType, String accessToken, long expiresIn, Instant accessTokenExpiresAt,
                                String refreshToken, Instant refreshTokenExpiresAt) {
        public static TokenResponse from(TokenPair p, Instant now) {
            return new TokenResponse("Bearer", p.accessToken(),
                    Math.max(0, p.accessTokenExpiresAt().getEpochSecond() - now.getEpochSecond()),
                    p.accessTokenExpiresAt(), p.refreshToken(), p.refreshTokenExpiresAt());
        }
    }

    public record RegisterTenantRequest(
            @NotBlank @Size(min = 2, max = 200) @Schema(example = "Globex Ltd") String tenantName,
            @NotBlank @Pattern(regexp = "^[a-z0-9][a-z0-9-]{1,61}[a-z0-9]$",
                    message = "3-63 lowercase letters, digits or hyphens; must start and end with a letter or digit")
            @Schema(example = "globex") String slug,
            @NotNull @Valid AdminAccount admin) {

        public record AdminAccount(
                @NotBlank @Email @Size(max = 254) String email,
                @NotBlank @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_MESSAGE) String password,
                @NotBlank @Size(max = 120) String displayName) {}
    }

    public record RegisterTenantResponse(UUID tenantId, String slug, UUID adminUserId) {}

    public record CreateAccountRequest(
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 120) String displayName,
            @NotBlank @Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_MESSAGE) String password,
            Set<@Pattern(regexp = "^[A-Z][A-Z0-9_]{1,49}$") String> roles) {}

    public record ChangeStatusRequest(@NotNull @Schema(allowableValues = {"ACTIVE", "DISABLED"}) String status) {}

    public record AccountResponse(UUID id, UUID tenantId, String email, String displayName, String status,
                                  List<String> roles, Instant lastLoginAt, Instant createdAt) {}

    public record MeResponse(UUID userId, UUID tenantId, String tenantSlug, String email, String displayName,
                             List<String> roles, List<String> permissions) {}

    public record RoleResponse(UUID id, String code, String name, String description, boolean system,
                               List<String> permissions) {}

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {}
}
