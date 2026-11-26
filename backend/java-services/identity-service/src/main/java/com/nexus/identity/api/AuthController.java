package com.nexus.identity.api;

import com.nexus.identity.api.dto.AuthDtos.*;
import com.nexus.identity.application.AuthService;
import com.nexus.identity.infrastructure.persistence.TenantRepository;
import com.nexus.identity.infrastructure.persistence.UserAccountRepository;
import com.nexus.common.error.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
public class AuthController {
    private final AuthService auth;
    private final UserAccountRepository users;
    private final TenantRepository tenants;
    private final Clock clock;

    public AuthController(AuthService auth, UserAccountRepository users, TenantRepository tenants, Clock clock) {
        this.auth = auth;
        this.users = users;
        this.tenants = tenants;
        this.clock = clock;
    }

    @PostMapping("/login")
    @Operation(summary = "Log in with tenant slug, email and password; returns an access token and a refresh token")
    public TokenResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        return TokenResponse.from(auth.login(req.tenant(), req.email(), req.password(),
                RequestMetadataResolver.from(http)), clock.instant());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a refresh token for a new token pair (the old refresh token is revoked)")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest req, HttpServletRequest http) {
        return TokenResponse.from(auth.refresh(req.refreshToken(), RequestMetadataResolver.from(http)), clock.instant());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoke the refresh token's login session")
    public void logout(@Valid @RequestBody RefreshRequest req, HttpServletRequest http) {
        auth.logout(req.refreshToken(), RequestMetadataResolver.from(http));
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    @Operation(summary = "Current user, tenant, roles and effective permissions")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        CurrentUser me = CurrentUser.from(jwt);
        var user = users.findByIdAndTenantId(me.userId(), me.tenantId())
                .orElseThrow(() -> ApiException.notFound("Account"));
        var tenant = tenants.findById(me.tenantId()).orElseThrow(() -> ApiException.notFound("Tenant"));
        return new MeResponse(user.getId(), tenant.getId(), tenant.getSlug(), user.getEmail(), user.getDisplayName(),
                List.copyOf(user.roleCodes()), List.copyOf(user.permissions()));
    }
}
