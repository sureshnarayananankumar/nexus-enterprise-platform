package com.nexus.identity;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexus.identity.domain.AuthAuditEntry;
import com.nexus.identity.infrastructure.persistence.AuthAuditRepository;
import com.nexus.identity.infrastructure.persistence.OutboxEventRepository;
import com.nexus.identity.infrastructure.persistence.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class AuthFlowIT extends AbstractIntegrationTest {

    @Autowired TenantRepository tenants;
    @Autowired AuthAuditRepository audit;
    @Autowired OutboxEventRepository outbox;

    @Test
    void tenantSignupThenLoginReturnsTokensAndMeShowsAdminPermissions() throws Exception {
        Map<String, String> t = registerTenant();
        JsonNode tokens = login(t.get("slug"), t.get("email"), PASSWORD);

        assertThat(tokens.get("tokenType").asText()).isEqualTo("Bearer");
        assertThat(tokens.get("expiresIn").asLong()).isBetween(800L, 900L);
        assertThat(tokens.get("refreshToken").asText()).hasSize(43);

        MvcResult me = perform(get("/api/v1/auth/me"), null, tokens.get("accessToken").asText());
        assertThat(me.getResponse().getStatus()).isEqualTo(200);
        JsonNode meBody = body(me);
        assertThat(meBody.get("tenantSlug").asText()).isEqualTo(t.get("slug"));
        assertThat(meBody.get("roles").toString()).contains("TENANT_ADMIN");
        assertThat(meBody.get("permissions").toString()).contains("identity.user.create", "identity.role.read");
    }

    @Test
    void signupWritesOutboxEventsAndAuditEntry() throws Exception {
        Map<String, String> t = registerTenant();
        var tenant = tenants.findBySlug(t.get("slug")).orElseThrow();

        assertThat(outbox.findByTenantIdOrderByOccurredAt(tenant.getId()))
                .extracting(e -> e.getEventType())
                .containsExactlyInAnyOrder("TenantCreated", "UserRegistered");
        assertThat(audit.findAll()).anyMatch(e -> e.getEventType() == AuthAuditEntry.Type.TENANT_CREATED
                && tenant.getId().equals(e.getTenantId()));
    }

    @Test
    void duplicateSlugIsRejectedWith409() throws Exception {
        Map<String, String> t = registerTenant();
        MvcResult r = perform(post("/api/v1/tenants"), Map.of("tenantName", "Again", "slug", t.get("slug"),
                "admin", Map.of("email", "x@y.test", "password", PASSWORD, "displayName", "X")), null);
        assertThat(r.getResponse().getStatus()).isEqualTo(409);
        assertThat(body(r).get("code").asText()).isEqualTo("TENANT_SLUG_TAKEN");
    }

    @Test
    void invalidSignupReturnsProblemDetailWithFieldErrors() throws Exception {
        MvcResult r = perform(post("/api/v1/tenants"), Map.of("tenantName", "", "slug", "Bad Slug!",
                "admin", Map.of("email", "not-an-email", "password", "short", "displayName", "X")), null);
        assertThat(r.getResponse().getStatus()).isEqualTo(400);
        assertThat(r.getResponse().getContentType()).contains("application/problem+json");
        JsonNode b = body(r);
        assertThat(b.get("code").asText()).isEqualTo("VALIDATION_ERROR");
        assertThat(b.get("errors").toString()).contains("slug", "admin.email", "admin.password", "tenantName");
    }

    @Test
    void wrongPasswordAndUnknownUserGiveTheSameError() throws Exception {
        Map<String, String> t = registerTenant();
        MvcResult wrongPw = perform(post("/api/v1/auth/login"),
                Map.of("tenant", t.get("slug"), "email", t.get("email"), "password", "WrongPassword1"), null);
        MvcResult unknown = perform(post("/api/v1/auth/login"),
                Map.of("tenant", t.get("slug"), "email", "nobody@x.test", "password", "WrongPassword1"), null);

        assertThat(wrongPw.getResponse().getStatus()).isEqualTo(401);
        assertThat(unknown.getResponse().getStatus()).isEqualTo(401);
        assertThat(body(wrongPw).get("detail")).isEqualTo(body(unknown).get("detail"));
    }

    @Test
    void accountLocksAfterFiveFailuresEvenForCorrectPassword() throws Exception {
        Map<String, String> t = registerTenant();
        for (int i = 0; i < 5; i++) {
            perform(post("/api/v1/auth/login"),
                    Map.of("tenant", t.get("slug"), "email", t.get("email"), "password", "WrongPassword1"), null);
        }
        MvcResult r = perform(post("/api/v1/auth/login"),
                Map.of("tenant", t.get("slug"), "email", t.get("email"), "password", PASSWORD), null);
        assertThat(r.getResponse().getStatus()).isEqualTo(401);
        assertThat(audit.findAll()).anyMatch(e -> e.getEventType() == AuthAuditEntry.Type.ACCOUNT_LOCKED);
    }

    @Test
    void refreshRotatesTokensAndReuseRevokesTheWholeFamily() throws Exception {
        Map<String, String> t = registerTenant();
        String r1 = login(t.get("slug"), t.get("email"), PASSWORD).get("refreshToken").asText();

        MvcResult first = perform(post("/api/v1/auth/refresh"), Map.of("refreshToken", r1), null);
        assertThat(first.getResponse().getStatus()).isEqualTo(200);
        String r2 = body(first).get("refreshToken").asText();
        assertThat(r2).isNotEqualTo(r1);

        // Replaying R1 (e.g. stolen) is detected...
        MvcResult replay = perform(post("/api/v1/auth/refresh"), Map.of("refreshToken", r1), null);
        assertThat(replay.getResponse().getStatus()).isEqualTo(401);
        assertThat(body(replay).get("code").asText()).isEqualTo("REFRESH_TOKEN_REUSED");

        // ...and the legitimate successor R2 is revoked too.
        MvcResult afterReuse = perform(post("/api/v1/auth/refresh"), Map.of("refreshToken", r2), null);
        assertThat(afterReuse.getResponse().getStatus()).isEqualTo(401);
        assertThat(body(afterReuse).get("code").asText()).isEqualTo("INVALID_REFRESH_TOKEN");
    }

    @Test
    void logoutRevokesRefreshToken() throws Exception {
        Map<String, String> t = registerTenant();
        String r1 = login(t.get("slug"), t.get("email"), PASSWORD).get("refreshToken").asText();

        assertThat(perform(post("/api/v1/auth/logout"), Map.of("refreshToken", r1), null).getResponse().getStatus())
                .isEqualTo(204);
        MvcResult r = perform(post("/api/v1/auth/refresh"), Map.of("refreshToken", r1), null);
        assertThat(r.getResponse().getStatus()).isEqualTo(401);
        assertThat(body(r).get("code").asText()).isEqualTo("INVALID_REFRESH_TOKEN");
    }

    @Test
    void protectedEndpointWithoutTokenReturns401Problem() throws Exception {
        MvcResult r = perform(get("/api/v1/auth/me"), null, null);
        assertThat(r.getResponse().getStatus()).isEqualTo(401);
        assertThat(body(r).get("code").asText()).isEqualTo("UNAUTHENTICATED");
    }

    @Test
    void tamperedTokenIsRejected() throws Exception {
        Map<String, String> t = registerTenant();
        String access = login(t.get("slug"), t.get("email"), PASSWORD).get("accessToken").asText();
        String tampered = access.substring(0, access.length() - 4) + "AAAA";
        assertThat(perform(get("/api/v1/auth/me"), null, tampered).getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void jwksPublishesThePublicKeyUsedToSignTokens() throws Exception {
        MvcResult r = perform(get("/.well-known/jwks.json"), null, null);
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        JsonNode key = body(r).get("keys").get(0);
        assertThat(key.get("kty").asText()).isEqualTo("RSA");
        assertThat(key.get("kid").asText()).isEqualTo("nexus-dev-key");
        assertThat(key.has("d")).as("private exponent must never be published").isFalse();
    }
}
