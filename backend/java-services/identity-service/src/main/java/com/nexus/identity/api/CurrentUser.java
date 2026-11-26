package com.nexus.identity.api;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/** Typed view of the authenticated caller, taken only from the verified JWT. */
record CurrentUser(UUID userId, UUID tenantId) {
    static CurrentUser from(Jwt jwt) {
        return new CurrentUser(UUID.fromString(jwt.getSubject()), UUID.fromString(jwt.getClaimAsString("tid")));
    }
}
