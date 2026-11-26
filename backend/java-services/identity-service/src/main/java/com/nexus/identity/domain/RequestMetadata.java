package com.nexus.identity.domain;

/** Client information captured for audit and token records. */
public record RequestMetadata(String ip, String userAgent) {
    public String userAgentTruncated() {
        return userAgent == null ? null : userAgent.substring(0, Math.min(255, userAgent.length()));
    }
}
