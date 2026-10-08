package com.yep.config;

import java.nio.charset.StandardCharsets;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Type-safe holder for all YEP application settings (prefix {@code yep.*}).
 * Centralising configuration here keeps environment concerns out of the domain
 * and service layers.
 */
@Configuration
@ConfigurationProperties(prefix = "yep")
public class YepProperties {

    private final Jwt jwt = new Jwt();
    private final Cors cors = new Cors();
    private final Cookie cookie = new Cookie();

    public Jwt getJwt() {
        return jwt;
    }

    public Cors getCors() {
        return cors;
    }

    public Cookie getCookie() {
        return cookie;
    }

    public static class Jwt {
        /** Shared HMAC-SHA256 secret. Must be at least 32 characters long. */
        private String secret = "";
        /** Access-token lifetime in milliseconds (default 24h). */
        private long expirationMs = 86_400_000L;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpirationMs() {
            return expirationMs;
        }

        public void setExpirationMs(long expirationMs) {
            this.expirationMs = expirationMs;
        }

        public byte[] secretBytes() {
            if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
                throw new IllegalStateException(
                        "yep.jwt.secret (env JWT_SECRET) must be at least 32 characters long");
            }
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    }

    public static class Cors {
        /** Comma separated list of allowed frontend origins (the Vercel deployment). */
        private String allowedOrigins = "http://localhost:3000";

        public String getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        public String[] originArray() {
            if (allowedOrigins == null || allowedOrigins.isBlank()) {
                return new String[0];
            }
            return allowedOrigins.split(",");
        }
    }

    public static class Cookie {
        /** true in production (HTTPS only). */
        private boolean secure = false;

        public boolean isSecure() {
            return secure;
        }

        public void setSecure(boolean secure) {
            this.secure = secure;
        }
    }
}
