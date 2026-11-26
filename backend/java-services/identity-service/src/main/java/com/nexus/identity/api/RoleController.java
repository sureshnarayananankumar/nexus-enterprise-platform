package com.nexus.identity.api;

import com.nexus.identity.api.dto.AuthDtos.RoleResponse;
import com.nexus.identity.application.AccountService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@Tag(name = "Roles")
public class RoleController {
    private final AccountService accounts;

    public RoleController(AccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('identity.role.read')")
    public List<RoleResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return accounts.rolesFor(CurrentUser.from(jwt).tenantId()).stream()
                .map(r -> new RoleResponse(r.getId(), r.getCode(), r.getName(), r.getDescription(), r.isSystem(),
                        r.getPermissions().stream().sorted().toList()))
                .toList();
    }
}
