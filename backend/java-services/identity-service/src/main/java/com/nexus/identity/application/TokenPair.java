package com.nexus.identity.application;

import java.time.Instant;

public record TokenPair(String accessToken, Instant accessTokenExpiresAt, String refreshToken,
                        Instant refreshTokenExpiresAt) {}
