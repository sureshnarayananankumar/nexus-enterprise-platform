package com.nexus.identity;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

class RbacAndTenantIsolationIT extends AbstractIntegrationTest {

    @Test
    void adminCreatesEmployeeWhoCannotListAccounts() throws Exception {
        Map<String, String> t = registerTenant();
        String admin = login(t.get("slug"), t.get("email"), PASSWORD).get("accessToken").asText();

        MvcResult created = perform(post("/api/v1/accounts"), Map.of("email", "emp@" + t.get("slug") + ".test",
                "displayName", "Employee One", "password", PASSWORD, "roles", List.of("EMPLOYEE")), admin);
        assertThat(created.getResponse().getStatus()).isEqualTo(201);
        assertThat(created.getResponse().getHeader("Location")).startsWith("/api/v1/accounts/");

        String employee = login(t.get("slug"), "emp@" + t.get("slug") + ".test", PASSWORD).get("accessToken").asText();
        MvcResult denied = perform(get("/api/v1/accounts"), null, employee);
        assertThat(denied.getResponse().getStatus()).isEqualTo(403);
        assertThat(body(denied).get("code").asText()).isEqualTo("FORBIDDEN");

        MvcResult list = perform(get("/api/v1/accounts?size=10"), null, admin);
        assertThat(list.getResponse().getStatus()).isEqualTo(200);
        assertThat(body(list).get("totalElements").asLong()).isEqualTo(2);
    }

    @Test
    void duplicateEmailAndUnknownRoleAreRejected() throws Exception {
        Map<String, String> t = registerTenant();
        String admin = login(t.get("slug"), t.get("email"), PASSWORD).get("accessToken").asText();

        MvcResult dup = perform(post("/api/v1/accounts"), Map.of("email", t.get("email").toUpperCase(),
                "displayName", "Dup", "password", PASSWORD), admin);
        assertThat(dup.getResponse().getStatus()).isEqualTo(409);
        assertThat(body(dup).get("code").asText()).isEqualTo("EMAIL_TAKEN");

        MvcResult badRole = perform(post("/api/v1/accounts"), Map.of("email", "r@" + t.get("slug") + ".test",
                "displayName", "R", "password", PASSWORD, "roles", List.of("SUPER_HACKER")), admin);
        assertThat(badRole.getResponse().getStatus()).isEqualTo(400);
        assertThat(body(badRole).get("code").asText()).isEqualTo("UNKNOWN_ROLE");
    }

    @Test
    void tenantsCannotSeeEachOthersAccounts() throws Exception {
        Map<String, String> a = registerTenant();
        Map<String, String> b = registerTenant();
        String adminA = login(a.get("slug"), a.get("email"), PASSWORD).get("accessToken").asText();
        String adminB = login(b.get("slug"), b.get("email"), PASSWORD).get("accessToken").asText();

        String userInA = body(perform(get("/api/v1/auth/me"), null, adminA)).get("userId").asText();

        MvcResult cross = perform(get("/api/v1/accounts/" + userInA), null, adminB);
        assertThat(cross.getResponse().getStatus()).isEqualTo(404);

        JsonNode listB = body(perform(get("/api/v1/accounts"), null, adminB));
        assertThat(listB.get("totalElements").asLong()).isEqualTo(1);
        assertThat(listB.get("content").get(0).get("email").asText()).isEqualTo(b.get("email"));
    }

    @Test
    void sameEmailMayExistInDifferentTenants() throws Exception {
        Map<String, String> a = registerTenant();
        Map<String, String> b = registerTenant();
        String adminB = login(b.get("slug"), b.get("email"), PASSWORD).get("accessToken").asText();
        MvcResult r = perform(post("/api/v1/accounts"), Map.of("email", a.get("email"),
                "displayName", "Same email, other tenant", "password", PASSWORD), adminB);
        assertThat(r.getResponse().getStatus()).isEqualTo(201);
    }

    @Test
    void disablingAnAccountRevokesItsSessions() throws Exception {
        Map<String, String> t = registerTenant();
        String admin = login(t.get("slug"), t.get("email"), PASSWORD).get("accessToken").asText();
        String email = "leaver@" + t.get("slug") + ".test";
        String userId = body(perform(post("/api/v1/accounts"), Map.of("email", email, "displayName", "Leaver",
                "password", PASSWORD), admin)).get("id").asText();
        String refresh = login(t.get("slug"), email, PASSWORD).get("refreshToken").asText();

        MvcResult disabled = perform(put("/api/v1/accounts/" + userId + "/status"), Map.of("status", "DISABLED"), admin);
        assertThat(disabled.getResponse().getStatus()).isEqualTo(200);
        assertThat(body(disabled).get("status").asText()).isEqualTo("DISABLED");

        assertThat(perform(post("/api/v1/auth/refresh"), Map.of("refreshToken", refresh), null)
                .getResponse().getStatus()).isEqualTo(401);
        assertThat(perform(post("/api/v1/auth/login"), Map.of("tenant", t.get("slug"), "email", email,
                "password", PASSWORD), null).getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void adminCannotDisableThemselves() throws Exception {
        Map<String, String> t = registerTenant();
        String admin = login(t.get("slug"), t.get("email"), PASSWORD).get("accessToken").asText();
        String selfId = body(perform(get("/api/v1/auth/me"), null, admin)).get("userId").asText();
        MvcResult r = perform(put("/api/v1/accounts/" + selfId + "/status"), Map.of("status", "DISABLED"), admin);
        assertThat(r.getResponse().getStatus()).isEqualTo(409);
    }

    @Test
    void rolesEndpointListsSystemRolesWithPermissions() throws Exception {
        Map<String, String> t = registerTenant();
        String admin = login(t.get("slug"), t.get("email"), PASSWORD).get("accessToken").asText();
        JsonNode roles = body(perform(get("/api/v1/roles"), null, admin));
        assertThat(roles.findValuesAsText("code")).contains("TENANT_ADMIN", "MANAGER", "EMPLOYEE", "CUSTOMER", "AUDITOR");
    }

    @Test
    void pageSizeAbove100IsRejected() throws Exception {
        Map<String, String> t = registerTenant();
        String admin = login(t.get("slug"), t.get("email"), PASSWORD).get("accessToken").asText();
        assertThat(perform(get("/api/v1/accounts?size=500"), null, admin).getResponse().getStatus()).isEqualTo(400);
    }
}
