package com.yep.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yep.domain.enums.AccountStatus;
import com.yep.domain.enums.Role;
import com.yep.domain.platform.PasswordResetToken;
import com.yep.domain.user.Administrator;
import com.yep.domain.user.Facilitator;
import com.yep.domain.user.Mentor;
import com.yep.domain.user.PartnerOrganisation;
import com.yep.domain.user.User;
import com.yep.domain.user.YouthParticipant;
import com.yep.dto.AuthDtos.AuthResponse;
import com.yep.dto.AuthDtos.ChangePasswordRequest;
import com.yep.dto.AuthDtos.ConsentRequest;
import com.yep.dto.AuthDtos.ForgotPasswordRequest;
import com.yep.dto.AuthDtos.LoginRequest;
import com.yep.dto.AuthDtos.MessageResponse;
import com.yep.dto.AuthDtos.RegisterRequest;
import com.yep.dto.AuthDtos.RegisterResponse;
import com.yep.dto.AuthDtos.ResetPasswordRequest;
import com.yep.dto.AuthDtos.UpdateProfileRequest;
import com.yep.dto.AuthDtos.UserProfile;
import com.yep.exception.BadRequestException;
import com.yep.exception.ConflictException;
import com.yep.exception.ForbiddenException;
import com.yep.exception.NotFoundException;
import com.yep.exception.UnauthorizedException;
import com.yep.repository.PasswordResetTokenRepository;
import com.yep.repository.UserRepository;
import com.yep.security.AuthenticatedUser;
import com.yep.security.JwtService;
import com.yep.security.LoginRateLimiter;
import com.yep.security.SecurityUtils;

@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginRateLimiter rateLimiter;
    private final SecurityUtils securityUtils;
    private final UserMapper userMapper;
    private final AuditService auditService;
    private final EmailGateway emailGateway;
    private final SettingsService settingsService;
    private final com.yep.config.YepProperties properties;

    public AuthService(UserRepository userRepository, PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder, JwtService jwtService, LoginRateLimiter rateLimiter,
            SecurityUtils securityUtils, UserMapper userMapper, AuditService auditService, EmailGateway emailGateway,
            SettingsService settingsService, com.yep.config.YepProperties properties) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.rateLimiter = rateLimiter;
        this.securityUtils = securityUtils;
        this.userMapper = userMapper;
        this.auditService = auditService;
        this.emailGateway = emailGateway;
        this.settingsService = settingsService;
        this.properties = properties;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (request.role() == Role.ADMIN) {
            throw new ForbiddenException("ADMIN_REGISTRATION_BLOCKED", "Administrator accounts are created by an administrator.");
        }
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("EMAIL_IN_USE", "An account with this email already exists.");
        }
        User user = buildUser(request.role(), request.name().trim(), email, request.password());
        applyRegistrationFields(user, request);
        user.register();
        user = userRepository.save(user);
        auditService.record(user, "REGISTER", "User", user.getUserId(), "Registered as " + user.getRole());
        String message = user.isPending()
                ? "Registration received. An administrator will review your account."
                : "Account created. You can now sign in.";
        return new RegisterResponse(user.getUserId(), user.getName(), user.getEmail(), user.getRole(),
                user.getStatus(), message);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress) {
        String email = normalizeEmail(request.email());
        rateLimiter.checkAndRecord(ipAddress, email);
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("INVALID_CREDENTIALS", "Email or password is incorrect."));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new UnauthorizedException("INVALID_CREDENTIALS", "Email or password is incorrect.");
        }
        if (user.isPending()) {
            throw new ForbiddenException("ACCOUNT_PENDING", "Your account is awaiting administrator approval.");
        }
        if (user.isSuspended()) {
            throw new ForbiddenException("ACCOUNT_SUSPENDED", "Your account has been suspended. Please contact support.");
        }
        user.login();
        userRepository.save(user);
        rateLimiter.clear(ipAddress, email);
        auditService.record(user, "LOGIN", "User", user.getUserId(), "Successful login");
        String token = jwtService.generate(user);
        return new AuthResponse(token, "Bearer", jwtService.expiresInSeconds(), userMapper.toProfile(user));
    }

    @Transactional
    public MessageResponse logout() {
        User user = currentUser();
        user.logout();
        userRepository.save(user);
        auditService.record(user, "LOGOUT", "User", user.getUserId(), "Client token discarded");
        return new MessageResponse("You have been signed out.");
    }

    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.email());
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            String rawToken = generateToken();
            tokenRepository.save(new PasswordResetToken(user, hashToken(rawToken),
                    OffsetDateTime.now().plusHours(1)));
            String link = properties.getFrontendUrl() + "/reset-password?token=" + rawToken;
            emailGateway.send(user.getEmail(), "Reset your YEP password",
                    "Hello " + user.getName() + ",\n\nUse this link within one hour to reset your password:\n"
                            + link + "\n\nIf you did not request this, you can ignore this email.");
            auditService.record(user, "PASSWORD_RESET_REQUESTED", "User", user.getUserId(), "Reset email requested");
        });
        return new MessageResponse("If an account exists for that email, a reset link has been sent.");
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = tokenRepository.findByTokenHash(hashToken(request.token()))
                .orElseThrow(() -> new BadRequestException("INVALID_RESET_TOKEN",
                        "This password reset link is invalid or has expired."));
        if (!token.isValid(OffsetDateTime.now())) {
            throw new BadRequestException("INVALID_RESET_TOKEN", "This password reset link is invalid or has expired.");
        }
        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        token.use();
        userRepository.save(user);
        tokenRepository.save(token);
        auditService.record(user, "PASSWORD_RESET", "User", user.getUserId(), "Password reset completed");
        return new MessageResponse("Your password has been reset. You can now sign in.");
    }

    @Transactional(readOnly = true)
    public UserProfile me() {
        return userMapper.toProfile(currentUser());
    }

    /** GET /users/{id}: a user may read their own profile; only an administrator may read others'. */
    @Transactional(readOnly = true)
    public UserProfile profile(Long userId) {
        AuthenticatedUser me = securityUtils.currentUser();
        if (!me.userId().equals(userId) && me.role() != Role.ADMIN) {
            throw new ForbiddenException("FORBIDDEN", "You can only view your own profile.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found."));
        return userMapper.toProfile(user);
    }

    @Transactional
    public UserProfile updateProfile(UpdateProfileRequest request) {
        User user = currentUser();
        if (request.name() != null) {
            String name = request.name().trim();
            if (name.isEmpty()) {
                throw new BadRequestException("VALIDATION_FAILED", "Name cannot be blank.");
            }
            user.setName(name);
        }
        if (request.phone() != null) {
            user.setPhone(trimToNull(request.phone()));
        }

        if (user instanceof YouthParticipant participant) {
            if (request.age() != null) {
                validateAge(request.age());
                participant.setAge(request.age());
            }
            if (request.country() != null) {
                participant.setCountry(validateCountry(request.country()));
            }
            if (request.digitalSkills() != null) {
                participant.setDigitalSkills(trimToNull(request.digitalSkills()));
            }
            if (request.institution() != null) {
                participant.setInstitution(trimToNull(request.institution()));
            }
            if (request.bio() != null) {
                participant.setBio(trimToNull(request.bio()));
            }
            if (request.shareProfileWithPartners() != null) {
                participant.setShareProfileWithPartners(request.shareProfileWithPartners());
                if (request.shareProfileWithPartners() && participant.getConsentDate() == null) {
                    participant.setConsentDate(OffsetDateTime.now());
                }
            }
        } else if (user instanceof Facilitator facilitator) {
            if (request.specialisation() != null) {
                facilitator.setSpecialisation(trimToNull(request.specialisation()));
            }
        } else if (user instanceof Mentor mentor) {
            if (request.organisation() != null) {
                mentor.setOrganisation(trimToNull(request.organisation()));
            }
            if (request.expertise() != null) {
                mentor.setExpertise(trimToNull(request.expertise()));
            }
        } else if (user instanceof Administrator administrator) {
            if (request.department() != null) {
                administrator.setDepartment(trimToNull(request.department()));
            }
        } else if (user instanceof PartnerOrganisation partner) {
            if (request.organisationName() != null) {
                String organisationName = request.organisationName().trim();
                if (organisationName.isEmpty()) {
                    throw new BadRequestException("VALIDATION_FAILED", "Organisation name cannot be blank.");
                }
                partner.setOrganisationName(organisationName);
            }
            if (request.country() != null) {
                partner.setCountry(validateCountry(request.country()));
            }
            if (request.organisationType() != null) {
                partner.setOrganisationType(request.organisationType());
            }
            if (request.website() != null) {
                partner.setWebsite(trimToNull(request.website()));
            }
            if (request.description() != null) {
                partner.setDescription(trimToNull(request.description()));
            }
        }

        userRepository.save(user);
        auditService.record(user, "PROFILE_UPDATED", "User", user.getUserId(), "Profile updated");
        return userMapper.toProfile(user);
    }

    @Transactional
    public UserProfile updateConsent(ConsentRequest request) {
        User user = currentUser();
        if (!(user instanceof YouthParticipant participant)) {
            throw new BadRequestException("CONSENT_NOT_APPLICABLE", "Only youth participants manage profile sharing consent.");
        }
        participant.setShareProfileWithPartners(request.shareProfileWithPartners());
        if (request.shareProfileWithPartners()) {
            participant.setConsentDate(OffsetDateTime.now());
        }
        userRepository.save(participant);
        auditService.record(participant, "CONSENT_UPDATED", "User", participant.getUserId(),
                request.shareProfileWithPartners() ? "Profile sharing enabled" : "Profile sharing disabled");
        return userMapper.toProfile(participant);
    }

    @Transactional
    public MessageResponse changePassword(ChangePasswordRequest request) {
        User user = currentUser();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadRequestException("INVALID_CURRENT_PASSWORD", "Your current password is incorrect.");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BadRequestException("PASSWORD_UNCHANGED", "Choose a password different from your current password.");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        auditService.record(user, "PASSWORD_CHANGED", "User", user.getUserId(), "Password changed");
        return new MessageResponse("Your password has been changed.");
    }

    private User buildUser(Role role, String name, String email, String password) {
        String hash = passwordEncoder.encode(password);
        return switch (role) {
            case PARTICIPANT -> new YouthParticipant();
            case FACILITATOR -> new Facilitator();
            case MENTOR -> new Mentor();
            case PARTNER -> new PartnerOrganisation();
            case ADMIN -> new Administrator();
        };
    }

    private void applyRegistrationFields(User user, RegisterRequest request) {
        user.setName(request.name().trim());
        user.setEmail(normalizeEmail(request.email()));
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPhone(trimToNull(request.phone()));

        if (user instanceof YouthParticipant participant) {
            if (request.age() == null) {
                throw new BadRequestException("VALIDATION_FAILED", "Age is required for youth participants.");
            }
            validateAge(request.age());
            participant.setAge(request.age());
            participant.setCountry(validateCountry(request.country()));
            participant.setDigitalSkills(trimToNull(request.digitalSkills()));
            participant.setInstitution(trimToNull(request.institution()));
            participant.setBio(trimToNull(request.bio()));
            boolean share = Boolean.TRUE.equals(request.shareProfileWithPartners());
            participant.setShareProfileWithPartners(share);
            if (share) {
                participant.setConsentDate(OffsetDateTime.now());
            }
        } else if (user instanceof Facilitator facilitator) {
            facilitator.setSpecialisation(trimToNull(request.specialisation()));
        } else if (user instanceof Mentor mentor) {
            mentor.setOrganisation(trimToNull(request.organisation()));
            mentor.setExpertise(trimToNull(request.expertise()));
        } else if (user instanceof Administrator administrator) {
            administrator.setDepartment(trimToNull(request.department()));
        } else if (user instanceof PartnerOrganisation partner) {
            String organisationName = trimToNull(request.organisationName());
            if (organisationName == null) {
                throw new BadRequestException("VALIDATION_FAILED", "Organisation name is required for partners.");
            }
            partner.setOrganisationName(organisationName);
            partner.setCountry(validateCountry(request.country()));
            partner.setOrganisationType(request.organisationType());
            partner.setWebsite(trimToNull(request.website()));
            partner.setDescription(trimToNull(request.description()));
        }
    }

    private User currentUser() {
        return userRepository.findById(securityUtils.currentUserId())
                .orElseThrow(() -> new UnauthorizedException("AUTHENTICATION_REQUIRED", "Please sign in to continue."));
    }

    private void validateAge(int age) {
        if (age < 18 || age > 35) {
            throw new BadRequestException("INVALID_AGE", "Youth participants must be between 18 and 35 years old.");
        }
    }

    private String validateCountry(String country) {
        if (country == null || country.isBlank()) {
            throw new BadRequestException("INVALID_COUNTRY", "Please select a country.");
        }
        List<String> countries = settingsService.countryList();
        return countries.stream()
                .filter(candidate -> candidate.equalsIgnoreCase(country.trim()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("INVALID_COUNTRY", "Please select a country from the list."));
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
