package com.yep.domain.user;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Platform administrator with a granular, comma separated permission list
 * (kept as a single column so the schema matches the agreed {@code users} table).
 */
@Entity
@DiscriminatorValue("ADMIN")
public class Admin extends User {

    @Column(name = "permissions", length = 500)
    private String permissions = "users:read,users:write,exchanges:write,teams:write,certificates:write";

    public boolean hasPermission(String permission) {
        if (permissions == null || permissions.isBlank()) {
            return false;
        }
        return Arrays.stream(permissions.split(","))
                .map(String::trim)
                .anyMatch(p -> p.equalsIgnoreCase(permission) || p.equals("*"));
    }

    public void grantPermission(String permission) {
        Set<String> current = permissionSet();
        if (current.add(permission)) {
            this.permissions = String.join(",", current);
        }
    }

    public void revokePermission(String permission) {
        Set<String> current = permissionSet();
        if (current.remove(permission)) {
            this.permissions = String.join(",", current);
        }
    }

    public Set<String> permissionSet() {
        if (permissions == null || permissions.isBlank()) {
            return new LinkedHashSet<>();
        }
        return Arrays.stream(permissions.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public String getPermissions() {
        return permissions;
    }

    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }
}
