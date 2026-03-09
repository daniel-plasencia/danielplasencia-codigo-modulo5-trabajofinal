package com.tecsup.app.micro.user.infrastructure.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Proveedor de tokens JWT
 *
 * Paquete: com.tecsup.app.micro.user.infrastructure.config
 * Sesión 2 - Módulo 4: OAuth 2.0 y JWT
 *
 * Responsabilidades:
 *   - Generar tokens JWT con email y roles
 *   - Validar tokens JWT
 *   - Extraer claims (email, roles) de un token
 *
 * El JWT_SECRET viene de:
 *   - application.yaml: jwt.secret (desarrollo local)
 *   - K8s Secret: JWT_SECRET (Kubernetes)
 */
@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:3600000}")  // Default: 1 hora
    private long jwtExpiration;

    public String generateToken(UserDetails userDetails, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        claims.put("userId", userId);

        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extrae el email (subject) del token
     */
    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Extrae los roles del token
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = getClaims(token);
        return claims.get("roles", List.class);
    }

    /**
     * Valida que el token sea válido (firma correcta, no expirado)
     */
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token JWT expirado: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Token JWT malformado: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("Token JWT inválido: {}", e.getMessage());
        }
        return false;
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
