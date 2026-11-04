package com.example.myfschool.security;

import com.example.myfschool.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String ROLES_CLAIM = "roles";
    private static final String USER_ID_CLAIM = "userId";

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(properties.secret());
        } catch (RuntimeException exception) {
            throw new IllegalStateException("JWT_SECRET must be valid Base64", exception);
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must decode to at least 256 bits");
        }
        signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(User user, List<String> roles) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(properties.expirationSeconds());
        return Jwts.builder()
                .subject(user.getUsername())
                .issuer(properties.issuer())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .claim(USER_ID_CLAIM, user.getId())
                .claim(ROLES_CLAIM, roles)
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getPayload().getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        if (!(userDetails instanceof AuthenticatedUserPrincipal principal)) {
            return false;
        }

        Jws<Claims> parsedToken = parseClaims(token);
        Claims claims = parsedToken.getPayload();
        List<?> roles = claims.get(ROLES_CLAIM, List.class);
        Number userId = claims.get(USER_ID_CLAIM, Number.class);
        boolean supportedRole = roles != null && roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .anyMatch(role -> userDetails.getAuthorities().stream()
                        .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role)));
        return userDetails.getUsername().equals(claims.getSubject())
                && userId != null
                && principal.getUserId().longValue() == userId.longValue()
                && supportedRole;
    }

    public long getExpirationSeconds() {
        return properties.expirationSeconds();
    }

    private Jws<Claims> parseClaims(String token) {
        Jws<Claims> parsedToken = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(properties.issuer())
                .build()
                .parseSignedClaims(token);
        if (!Jwts.SIG.HS256.getId().equals(parsedToken.getHeader().getAlgorithm())) {
            throw new JwtException("Unsupported JWT signature algorithm");
        }
        return parsedToken;
    }
}
