package com.yep.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "yep")
public class YepProperties {

    private final Jwt jwt = new Jwt();
    private final Cors cors = new Cors();
    private final AfricasTalking africasTalking = new AfricasTalking();
    private String frontendUrl = "http://localhost:3000";
    private String mailFrom = "";

    public Jwt getJwt() {
        return jwt;
    }

    public Cors getCors() {
        return cors;
    }

    public AfricasTalking getAfricasTalking() {
        return africasTalking;
    }

    public String getFrontendUrl() {
        return frontendUrl;
    }

    public void setFrontendUrl(String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    public String getMailFrom() {
        return mailFrom;
    }

    public void setMailFrom(String mailFrom) {
        this.mailFrom = mailFrom;
    }

    public static class Jwt {
        private String secret = "dev-only-secret-change-me-to-a-long-random-string-32+";
        private int expiryHours = 8;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public int getExpiryHours() {
            return expiryHours;
        }

        public void setExpiryHours(int expiryHours) {
            this.expiryHours = expiryHours;
        }
    }

    public static class Cors {
        private String allowedOrigins = "http://localhost:3000";

        public String getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class AfricasTalking {
        private String username = "";
        private String apiKey = "";

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }
}
