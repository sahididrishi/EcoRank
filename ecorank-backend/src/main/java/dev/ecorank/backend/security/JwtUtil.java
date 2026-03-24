package dev.ecorank.backend.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import dev.ecorank.backend.config.EcoRankProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    private final SecretKey signingKey;
    private final long accessTokenExpiryMs;

    public JwtUtil(EcoRankProperties properties) {
        byte[] keyBytes = properties.getJwt().getSecret().getBytes();
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpiryMs = properties.getJwt().getAccessTokenExpiryMinutes() * 60L * 1000L;
    }

    public String generateAccessToken(String username, Long adminId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpiryMs);

        return Jwts.builder()
                .subject(username)
                .claim("adminId", adminId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    public long getAccessTokenExpirySeconds() {
        return accessTokenExpiryMs / 1000;
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }

    public Long getAdminIdFromToken(String token) {
        return parseToken(token).get("adminId", Long.class);
    }
}
