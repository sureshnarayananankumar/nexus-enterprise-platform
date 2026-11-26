package com.nexus.identity.api;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

/** Public signing keys used by the gateway and every resource server to verify access tokens. */
@RestController
@Tag(name = "Keys")
public class JwksController {
    private final JWKSet publicSet;

    public JwksController(RSAKey signingKey) {
        this.publicSet = new JWKSet(signingKey.toPublicJWK());
    }

    @GetMapping("/.well-known/jwks.json")
    @Operation(summary = "JSON Web Key Set (public keys only)")
    public ResponseEntity<Map<String, Object>> jwks() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic())
                .body(publicSet.toJSONObject());
    }
}
