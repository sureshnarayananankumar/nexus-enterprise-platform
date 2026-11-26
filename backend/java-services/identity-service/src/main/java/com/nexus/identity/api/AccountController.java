package com.nexus.identity.api;

import com.nexus.common.error.ApiException;
import com.nexus.identity.api.dto.AuthDtos.*;
import com.nexus.identity.application.AccountService;
import com.nexus.identity.domain.UserAccount;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Accounts")
@Validated
public class AccountController {
    private final AccountService accounts;

    public AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('identity.user.create')")
    @Operation(summary = "Create an account in the caller's tenant")
    public ResponseEntity<AccountResponse> create(@AuthenticationPrincipal Jwt jwt,
                                                  @Valid @RequestBody CreateAccountRequest req,
                                                  HttpServletRequest http) {
        CurrentUser me = CurrentUser.from(jwt);
        UserAccount u = accounts.create(me.tenantId(), me.userId(), req.email(), req.displayName(), req.password(),
                req.roles(), RequestMetadataResolver.from(http));
        return ResponseEntity.created(URI.create("/api/v1/accounts/" + u.getId())).body(toResponse(u));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('identity.user.read')")
    @Operation(summary = "List accounts in the caller's tenant (newest first)")
    public PageResponse<AccountResponse> list(@AuthenticationPrincipal Jwt jwt,
                                              @RequestParam(defaultValue = "0") @Min(0) int page,
                                              @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Page<UserAccount> result = accounts.list(CurrentUser.from(jwt).tenantId(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return new PageResponse<>(result.map(AccountController::toResponse).getContent(), result.getNumber(),
                result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('identity.user.read')")
    public AccountResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return toResponse(accounts.get(CurrentUser.from(jwt).tenantId(), id));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('identity.user.update')")
    @Operation(summary = "Enable or disable an account; disabling revokes all its refresh tokens")
    public AccountResponse changeStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                        @Valid @RequestBody ChangeStatusRequest req, HttpServletRequest http) {
        UserAccount.Status status;
        try {
            status = UserAccount.Status.valueOf(req.status());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("status must be ACTIVE or DISABLED");
        }
        CurrentUser me = CurrentUser.from(jwt);
        return toResponse(accounts.changeStatus(me.tenantId(), me.userId(), id, status, RequestMetadataResolver.from(http)));
    }

    static AccountResponse toResponse(UserAccount u) {
        return new AccountResponse(u.getId(), u.getTenantId(), u.getEmail(), u.getDisplayName(), u.getStatus().name(),
                List.copyOf(u.roleCodes()), u.getLastLoginAt(), u.getCreatedAt());
    }
}
