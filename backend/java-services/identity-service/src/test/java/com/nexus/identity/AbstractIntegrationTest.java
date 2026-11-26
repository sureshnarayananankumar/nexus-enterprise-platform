package com.nexus.identity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Boots the full service against a real PostgreSQL 16 (Flyway migrations applied). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    /**
     * Singleton container shared by all integration test classes (and the cached Spring context).
     * Started once per JVM; Testcontainers' Ryuk removes it when the JVM exits.
     */
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    protected static final String PASSWORD = "CorrectHorse42battery";

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;

    protected MvcResult perform(MockHttpServletRequestBuilder req, Object body, String token) throws Exception {
        req.contentType(MediaType.APPLICATION_JSON);
        if (body != null) req.content(json.writeValueAsString(body));
        if (token != null) req.header("Authorization", "Bearer " + token);
        return mvc.perform(req).andReturn();
    }

    protected JsonNode body(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString());
    }

    protected String uniqueSlug() {
        return "t-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /** Registers a tenant and returns {slug, adminEmail}. */
    protected Map<String, String> registerTenant() throws Exception {
        String slug = uniqueSlug();
        String email = "admin@" + slug + ".test";
        MvcResult r = perform(post("/api/v1/tenants"), Map.of(
                "tenantName", "Tenant " + slug,
                "slug", slug,
                "admin", Map.of("email", email, "password", PASSWORD, "displayName", "Admin " + slug)), null);
        if (r.getResponse().getStatus() != 201) {
            throw new AssertionError("Tenant registration failed: " + r.getResponse().getContentAsString());
        }
        return Map.of("slug", slug, "email", email);
    }

    protected JsonNode login(String slug, String email, String password) throws Exception {
        MvcResult r = perform(post("/api/v1/auth/login"),
                Map.of("tenant", slug, "email", email, "password", password), null);
        if (r.getResponse().getStatus() != 200) {
            throw new AssertionError("Login failed: " + r.getResponse().getContentAsString());
        }
        return body(r);
    }
}
