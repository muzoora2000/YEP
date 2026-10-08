package com.yep.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yep.domain.enums.AccountStatus;
import com.yep.domain.user.User;
import com.yep.repository.UserRepository;

/**
 * Bridges the {@link com.yep.domain.user.User} hierarchy onto Spring Security's
 * {@link UserDetails}: one account, one role, one BCrypt protected password.
 */
@Service
public class YepUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public YepUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email == null ? "" : email.trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("No account for " + email));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(stripEncodingPrefix(user.getPasswordHash()))
                .authorities("ROLE_" + user.getRole().name())
                .disabled(user.getAccountStatus() != AccountStatus.ACTIVE)
                .build();
    }

    /** Accounts seeded with an explicit {@code {bcrypt}} marker still verify. */
    private String stripEncodingPrefix(String passwordHash) {
        if (passwordHash != null && passwordHash.startsWith("{bcrypt}")) {
            return passwordHash.substring("{bcrypt}".length());
        }
        return passwordHash;
    }
}
