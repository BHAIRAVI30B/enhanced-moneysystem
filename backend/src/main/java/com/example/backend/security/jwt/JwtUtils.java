package com.example.backend.security.jwt;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import com.example.backend.security.service.UserDetailsImpl;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtils {

    @Value("${backend.jwtSecret}")
    private String jwtSecret;

    @Value("${backend.jwtExpirationMs}")
    private int jwtExpirationMs;

    // NEW: accepts sessionId and embeds it as a claim
    public String generateJwtToken(Authentication authentication, String sessionId) {
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();

        return Jwts.builder()
                .setSubject(userPrincipal.getUsername())
                .claim("accountId", userPrincipal.getAccountId())
                .claim("sessionId", sessionId)
                .claim("roles", userPrincipal.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList())
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(getSignInKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public String getAccountIdFromJwtToken(String token) {
        return (String) Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("accountId");
    }

    // NEW: extracts sessionId claim from token
    public String getSessionIdFromJwtToken(String token) {
        return (String) Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("sessionId");
    }

    public List<String> getRolesFromJwtToken(String token) {
        Object rolesObj = Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("roles");

        if (rolesObj instanceof List<?> list) {
            return list.stream()
                    .map(Object::toString)
                    .toList();
        }
        return List.of();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(getSignInKey()).build().parseClaimsJws(authToken);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Key getSignInKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
