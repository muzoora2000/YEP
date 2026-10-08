package com.yep.config;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

/**
 * Render and Neon hand out {@code DATABASE_URL} in {@code postgres://} form, which the
 * PostgreSQL JDBC driver does not understand. This {@link EnvironmentPostProcessor}
 * rewrites it to a valid JDBC URL and, when the credentials are embedded in the URL,
 * exposes them as {@code spring.datasource.username}/{@code spring.datasource.password}.
 *
 * <p>Registered through {@code META-INF/spring.factories}.</p>
 */
public class PostgresUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    static final String PROPERTY_SOURCE_NAME = "yepDatabaseUrl";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String raw = environment.getProperty("DATABASE_URL");
        if (raw == null || raw.isBlank() || raw.startsWith("jdbc:")) {
            return;
        }

        String jdbcUrl = raw.replaceFirst("^(postgres|postgresql)://", "jdbc:postgresql://");
        Map<String, Object> overrides = new HashMap<>();
        overrides.put("spring.datasource.url", jdbcUrl);

        String userInfo = userInfoOf(jdbcUrl);
        if (userInfo != null && !userInfo.isEmpty()) {
            String[] parts = userInfo.split(":", 2);
            jdbcUrl = jdbcUrl.replace("//" + userInfo + "@", "//");
            overrides.put("spring.datasource.url", jdbcUrl);
            if (parts.length == 2) {
                if (isBlank(environment.getProperty("spring.datasource.username"))) {
                    overrides.put("spring.datasource.username", decode(parts[0]));
                }
                if (isBlank(environment.getProperty("spring.datasource.password"))) {
                    overrides.put("spring.datasource.password", decode(parts[1]));
                }
            }
        }

        MutablePropertySources sources = environment.getPropertySources();
        if (sources.contains(PROPERTY_SOURCE_NAME)) {
            sources.remove(PROPERTY_SOURCE_NAME);
        }
        sources.addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, overrides));
    }

    private String userInfoOf(String jdbcUrl) {
        int schemeEnd = jdbcUrl.indexOf("://");
        if (schemeEnd < 0) {
            return null;
        }
        int at = jdbcUrl.indexOf('@', schemeEnd + 3);
        int path = jdbcUrl.indexOf('/', schemeEnd + 3);
        if (at < 0 || (path > -1 && at > path)) {
            return null;
        }
        return jdbcUrl.substring(schemeEnd + 3, at);
    }

    private String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return value;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
