package com.yep.domain.user;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.yep.domain.base.BaseEntity;
import com.yep.domain.enums.AccountStatus;
import com.yep.domain.enums.NotificationChannel;
import com.yep.domain.enums.Role;
import com.yep.domain.interfaces.Notifiable;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

/**
 * Abstract base class of every account on YEP.
 *
 * <p>Uses {@link InheritanceType#SINGLE_TABLE}: all four roles share the
 * {@code users} table and the {@code role} column doubles as the JPA
 * discriminator, exactly as required by the database schema.</p>
 *
 * <p>Encapsulates authentication and notification concerns behind
 * {@link UserDetails} and {@link Notifiable} so the security layer and the
 * notification layer depend only on abstractions.</p>
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = @jakarta.persistence.UniqueConstraint(name = "uk_users_email", columnNames = "email"),
        indexes = {
                @Index(name = "idx_users_role", columnList = "role"),
                @Index(name = "idx_users_account_status", columnList = "account_status"),
                @Index(name = "idx_users_created_at", columnList = "created_at")
        }
)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "role", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("USER")
public abstract class User extends BaseEntity implements UserDetails, Notifiable {

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "email", nullable = false, length = 190)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    /** Read-only mirror of the {@code role} discriminator column. */
    @Column(name = "role", insertable = false, updatable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "country", length = 80)
    private String country;

    @Column(name = "phone", length = 30)
    private String phone;

    /** Minimal-PII / consent requirement: registration is only allowed with explicit consent. */
    @Column(name = "consent_given", nullable = false)
    private boolean consentGiven;

    @Column(name = "account_status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    // ---------------------------------------------------------------------
    // UserDetails (Spring Security contract)
    // ---------------------------------------------------------------------

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role != null ? role.authority() : Role.PARTICIPANT.authority()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountStatus != AccountStatus.SUSPENDED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return accountStatus == AccountStatus.ACTIVE;
    }

    // ---------------------------------------------------------------------
    // Notifiable
    // ---------------------------------------------------------------------

    @Override
    public String getNotificationAddress() {
        return email;
    }

    @Override
    public NotificationChannel getPreferredChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public String getNotificationLabel() {
        return fullName;
    }

    // ---------------------------------------------------------------------
    // Convenience
    // ---------------------------------------------------------------------

    /** Role-based test used by services for ownership / authorization rules. */
    public boolean hasRole(Role required) {
        return role == required;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    // ---------------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------------

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isConsentGiven() {
        return consentGiven;
    }

    public void setConsentGiven(boolean consentGiven) {
        this.consentGiven = consentGiven;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }
}
