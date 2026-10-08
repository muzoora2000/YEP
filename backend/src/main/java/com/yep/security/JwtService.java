package com.yep.security;

import java.time.Duration;
import java.util.Date;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import com.yep.config.YepProperties;
import com.yep.domain.user.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Creates and validates HS256 JSON Web Tokens and manages the httpOnly cookie
 * that carries the token to the browser (low bandwidth friendly: one small header).
 */
@Service
public class JwtService {

    public static final String COOKIE_NAME = "yep_token";
    public static final String HEADER_PREFIX = "Bearer ";

    private final YepProperties properties;
    private final javax.crypto.SecretKey key;

    public JwtService(YepProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.getJwt().secretBytes());
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + properties.getJwt().getExpirationMs());
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("name", user.getFullName())
                .claim("role", user.getRole() != null ? user.getRole().name() : "")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /**
     * @return the principal encoded in the token
     * @throws JwtException when the token is invalid, expired or tampered with
     */
    public AuthenticatedUser parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long uid = claims.get("uid", Number.class) != null ? claims.get("uid", Number.class).longValue() : null;
        String role = claims.get("role", String.class);
        return new AuthenticatedUser(
                uid,
                claims.getSubject(),
                claims.get("name", String.class),
                role == null || role.isBlank() ? com.yep.domain.enums.Role.PARTICIPANT
                        : com.yep.domain.enums.Role.valueOf(role));
    }

    public long expiresInSeconds() {
        return Duration.ofMillis(properties.getJwt().getExpirationMs()).toSeconds();
    }

    /** Writes the JWT as a browser readable httpOnly cookie (cross-site safe in prod). */
    public void writeAuthCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .secure(properties.getCookie().isSecure())
                .path("/")
                .maxAge(Duration.ofMillis(properties.getJwt().getExpirationMs()))
                .sameSite(properties.getCookie().isSecure() ? "None" : "Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearAuthCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(properties.getCookie().isSecure())
                .path("/")
                .maxAge(Duration.ZERO)
                .sameSite(properties.getCookie().isSecure() ? "None" : "Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /** Reads the raw token from the Authorization header, falling back to the cookie. */
    public String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(HEADER_PREFIX)) {
            return header.substring(HEADER_PREFIX.length());
        }
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (COOKIE_NAME.equals(cookie.getName()) && cookie.getValue() != null
                        && !cookie.getValue().isBlank()) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
