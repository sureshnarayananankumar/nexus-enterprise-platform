package com.nexus.identity.api;

import com.nexus.identity.api.dto.AuthDtos.RegisterTenantRequest;
import com.nexus.identity.api.dto.AuthDtos.RegisterTenantResponse;
import com.nexus.identity.application.TenantRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/tenants")
@Tag(name = "Tenants")
public class TenantController {
    private final TenantRegistrationService registration;

    public TenantController(TenantRegistrationService registration) {
        this.registration = registration;
    }

    @PostMapping
    @Operation(summary = "Sign up a new tenant with its first administrator")
    public ResponseEntity<RegisterTenantResponse> register(@Valid @RequestBody RegisterTenantRequest req,
                                                           HttpServletRequest http) {
        var r = registration.register(req.tenantName(), req.slug(), req.admin().email(), req.admin().password(),
                req.admin().displayName(), RequestMetadataResolver.from(http));
        return ResponseEntity.created(URI.create("/api/v1/tenants/" + r.tenantId()))
                .body(new RegisterTenantResponse(r.tenantId(), r.slug(), r.adminUserId()));
    }
}
