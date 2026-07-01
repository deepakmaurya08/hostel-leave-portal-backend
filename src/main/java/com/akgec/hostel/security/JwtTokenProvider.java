package com.akgec.hostel.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT utility using jjwt 0.12.x API.
 *
 * Breaking changes from 0.11.x → 0.12.x:
 *  - Keys.hmacShaKeyFor()  →  same, but returns SecretKey (not Key)
 *  - Jwts.parserBuilder()  →  Jwts.parser()
 *  - .setSigningKey()      →  .verifyWith()
 *  - SignatureAlgorithm enum  →  Jwts.SIG.HS256 / MacAlgorithm
 *  - .parseClaimsJws()     →  .parseSignedClaims()
 *  - .getBody()            →  .getPayload()
 */
@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    // ── Build a SecretKey from the Base64-encoded secret ──────────────────
    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    // ── Generate a token for an authenticated user ────────────────────────
    public String generateToken(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return generateTokenFromEmail(userDetails.getEmail());
    }

    public String generateTokenFromEmail(String email) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(email)          // 0.12.x: subject() replaces setSubject()
                .issuedAt(now)           // 0.12.x: issuedAt() replaces setIssuedAt()
                .expiration(expiry)      // 0.12.x: expiration() replaces setExpiration()
                .signWith(signingKey())  // 0.12.x: algorithm inferred from key type
                .compact();
    }

    // ── Extract email claim from a valid token ────────────────────────────
    public String getEmailFromToken(String token) {
        return Jwts.parser()                   // 0.12.x: parser() replaces parserBuilder()
                .verifyWith(signingKey())       // 0.12.x: verifyWith() replaces setSigningKey()
                .build()
                .parseSignedClaims(token)       // 0.12.x: parseSignedClaims() replaces parseClaimsJws()
                .getPayload()                   // 0.12.x: getPayload() replaces getBody()
                .getSubject();
    }

    // ── Validate — returns true only if signature + expiry are both OK ────
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("JWT unsupported: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("JWT malformed: {}", e.getMessage());
        } catch (SecurityException e) {
            log.warn("JWT signature invalid: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims empty: {}", e.getMessage());
        }
        return false;
    }
}
