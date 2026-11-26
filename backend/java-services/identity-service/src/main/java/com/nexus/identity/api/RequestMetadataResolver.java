package com.nexus.identity.api;

import com.nexus.identity.domain.RequestMetadata;
import jakarta.servlet.http.HttpServletRequest;

/** Extracts client metadata. Behind the gateway, enable server.forward-headers-strategy so getRemoteAddr is the client IP. */
final class RequestMetadataResolver {
    private RequestMetadataResolver() {}

    static RequestMetadata from(HttpServletRequest req) {
        return new RequestMetadata(req.getRemoteAddr(), req.getHeader("User-Agent"));
    }
}
