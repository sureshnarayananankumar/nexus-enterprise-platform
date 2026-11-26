package com.nexus.identity.application;

import com.nexus.identity.config.IdentityProperties;
import com.nexus.identity.domain.UserAccount;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Builds and signs RS256 access tokens with the NEXUS claim set (sub, tid, email, roles, perms). */
@Component
public class AccessTokenIssuer {
    private final JwtEncoder encoder;
    private final IdentityProperties props;

    public AccessTokenIssuer(JwtEncoder encoder, IdentityProperties props) {
        this.encoder = encoder;
        this.props = props;
    }

    public IssuedToken issue(UserAccount user, Instant now) {
        Instant exp = now.plus(props.accessTokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.issuer())
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(exp)
                .id(UUID.randomUUID().toString())
                .claim("tid", user.getTenantId().toString())
                .claim("email", user.getEmail())
                .claim("name", user.getDisplayName())
                .claim("roles", List.copyOf(user.roleCodes()))
                .claim("perms", List.copyOf(user.permissions()))
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(props.jwt().keyId()).type("JWT").build();
        String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(value, exp);
    }

    public record IssuedToken(String value, Instant expiresAt) {}
}
