package com.zenius.one.modules.identity.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private final SecretKey signingKey;
    private final String issuer;
    private final long accessTokenMinutes;

    public JwtTokenService(
            @Value("${zenius.security.jwt.secret}") String secret,
            @Value("${zenius.security.jwt.issuer:https://auth.zeniusone.local}") String issuer,
            @Value("${zenius.security.jwt.access-token-minutes:15}") long accessTokenMinutes
    ) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("zenius.security.jwt.secret must be at least 32 bytes");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.issuer = issuer;
        this.accessTokenMinutes = accessTokenMinutes;
    }

    public String generateAccessToken(UUID userId, String email, UUID activeOrganizationId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(accessTokenMinutes, ChronoUnit.MINUTES);

        var builder = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(issuer)
                .subject(userId.toString())
                .claim("email", email)
                .claim("tokenType", "ACCESS")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt));

        if (activeOrganizationId != null) {
            builder.claim("orgId", activeOrganizationId.toString());
        }
        return builder.signWith(signingKey).compact();
    }

    public String generateMfaChallengeToken(UUID userId, String email) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(5, ChronoUnit.MINUTES);
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(issuer)
                .subject(userId.toString())
                .claim("email", email)
                .claim("tokenType", "MFA_CHALLENGE")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    public ParsedJwt parseAndVerify(String token, String expectedType) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String tokenType = claims.get("tokenType", String.class);
        if (!expectedType.equals(tokenType)) {
            throw new IllegalArgumentException("Unexpected token type");
        }

        UUID userId = UUID.fromString(claims.getSubject());
        String email = claims.get("email", String.class);
        String orgRaw = claims.get("orgId", String.class);
        UUID orgId = orgRaw != null ? UUID.fromString(orgRaw) : null;
        return new ParsedJwt(userId, email, orgId, claims.getId());
    }

    public record ParsedJwt(UUID userId, String email, UUID organizationId, String jti) {
    }
}

