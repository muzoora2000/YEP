package com.yep.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.yep.domain.enums.AccountStatus;
import com.yep.domain.user.Administrator;
import com.yep.repository.UserRepository;
import com.yep.service.AuditService;
import com.yep.service.SettingsService;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SettingsService settingsService;
    private final AuditService auditService;
    private final String adminEmail;
    private final String adminPassword;
    private final String adminName;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder,
            SettingsService settingsService, AuditService auditService,
            @Value("${ADMIN_EMAIL:}") String adminEmail,
            @Value("${ADMIN_PASSWORD:}") String adminPassword,
            @Value("${ADMIN_NAME:YEP Administrator}") String adminName) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.settingsService = settingsService;
        this.auditService = auditService;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.adminName = adminName;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        settingsService.seedDefaults();
        if (userRepository.count() > 0) {
            return;
        }
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            log.warn("No users exist and ADMIN_EMAIL/ADMIN_PASSWORD are not set. Set them to seed the first administrator.");
            return;
        }
        Administrator admin = new Administrator();
        admin.setName(adminName);
        admin.setEmail(adminEmail.trim().toLowerCase());
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setStatus(AccountStatus.ACTIVE);
        admin.setDepartment("Programme");
        admin = userRepository.save(admin);
        auditService.record(admin, "ADMIN_SEEDED", "User", admin.getUserId(), "First administrator created from environment");
        log.info("Seeded first administrator: {}", admin.getEmail());
    }
}
