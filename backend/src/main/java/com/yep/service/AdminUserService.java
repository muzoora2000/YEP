package com.yep.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yep.domain.enums.AccountStatus;
import com.yep.domain.enums.Role;
import com.yep.domain.user.Administrator;
import com.yep.domain.user.Facilitator;
import com.yep.domain.user.Mentor;
import com.yep.domain.user.PartnerOrganisation;
import com.yep.domain.user.User;
import com.yep.domain.user.YouthParticipant;
import com.yep.dto.AdminDtos.CreateUserRequest;
import com.yep.dto.AdminDtos.RoleChangeRequest;
import com.yep.dto.AdminDtos.StatusChangeRequest;
import com.yep.dto.AuthDtos.UserProfile;
import com.yep.exception.BadRequestException;
import com.yep.exception.BusinessRuleException;
import com.yep.exception.ConflictException;
import com.yep.exception.ForbiddenException;
import com.yep.exception.NotFoundException;
import com.yep.repository.LearningActivityRepository;
import com.yep.repository.ProjectReviewRepository;
import com.yep.repository.TeamRepository;
import com.yep.repository.UserRepository;
import com.yep.security.SecurityUtils;

import jakarta.persistence.EntityManager;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuditService auditService;
    private final SettingsService settingsService;
    private final SecurityUtils securityUtils;
    private final LearningActivityRepository activityRepository;
    private final TeamRepository teamRepository;
    private final ProjectReviewRepository projectReviewRepository;
    private final EntityManager entityManager;

    public AdminUserService(UserRepository userRepository, PasswordEncoder passwordEncoder, UserMapper userMapper,
            AuditService auditService, SettingsService settingsService, SecurityUtils securityUtils,
            LearningActivityRepository activityRepository, TeamRepository teamRepository,
            ProjectReviewRepository projectReviewRepository, EntityManager entityManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.auditService = auditService;
        this.settingsService = settingsService;
        this.securityUtils = securityUtils;
        this.activityRepository = activityRepository;
        this.teamRepository = teamRepository;
        this.projectReviewRepository = projectReviewRepository;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public Page<UserProfile> list(Role role, AccountStatus status, String query, Pageable pageable) {
        String search = query == null || query.isBlank() ? null : query.trim();
        return userRepository.search(role, status, search, pageable).map(userMapper::toProfile);
    }

    @Transactional(readOnly = true)
    public UserProfile get(Long userId) {
        return userMapper.toProfile(find(userId));
    }

    @Transactional
    public UserProfile create(CreateUserRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("EMAIL_IN_USE", "An account with this email already exists.");
        }
        User user = switch (request.role()) {
            case PARTICIPANT -> new YouthParticipant();
            case FACILITATOR -> new Facilitator();
            case MENTOR -> new Mentor();
            case PARTNER -> new PartnerOrganisation();
            case ADMIN -> new Administrator();
        };
        user.setName(request.name().trim());
        user.setEmail(email);
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
            partner.setVerified(true);
        }

        user.setStatus(AccountStatus.ACTIVE);
        user = userRepository.save(user);
        auditService.record(actor(), "USER_CREATED", "User", user.getUserId(), "Admin created " + user.getRole());
        return userMapper.toProfile(user);
    }

    @Transactional
    public UserProfile changeStatus(Long userId, StatusChangeRequest request) {
        User user = find(userId);
        if (request.status() == AccountStatus.SUSPENDED) {
            if (userId.equals(securityUtils.currentUserId())) {
                throw new BusinessRuleException("CANNOT_SUSPEND_SELF", "You cannot suspend your own account.");
            }
            if (user.getRole() == Role.ADMIN && user.getStatus() == AccountStatus.ACTIVE
                    && userRepository.countByRole(Role.ADMIN) <= 1) {
                throw new BusinessRuleException("LAST_ADMIN", "The last active administrator cannot be suspended.");
            }
        }
        user.setStatus(request.status());
        userRepository.save(user);
        auditService.record(actor(), "USER_STATUS_CHANGED", "User", userId, "Status set to " + request.status());
        return userMapper.toProfile(user);
    }

    @Transactional
    public UserProfile approve(Long userId, boolean approved) {
        User user = find(userId);
        user.setStatus(approved ? AccountStatus.ACTIVE : AccountStatus.SUSPENDED);
        if (approved && user instanceof PartnerOrganisation partner) {
            partner.setVerified(true);
        }
        userRepository.save(user);
        auditService.record(actor(), approved ? "USER_APPROVED" : "USER_REJECTED", "User", userId,
                approved ? "Account approved" : "Account rejected");
        return userMapper.toProfile(user);
    }

    @Transactional
    public UserProfile changeRole(Long userId, RoleChangeRequest request) {
        User user = find(userId);
        Role target = request.role();
        if (target == user.getRole()) {
            return userMapper.toProfile(user);
        }
        if (userId.equals(securityUtils.currentUserId())) {
            throw new BusinessRuleException("CANNOT_CHANGE_OWN_ROLE", "You cannot change your own role.");
        }
        if (target == Role.ADMIN || user.getRole() == Role.ADMIN) {
            throw new BusinessRuleException("ROLE_CHANGE_NOT_ALLOWED",
                    "Administrator roles cannot be changed through this operation.");
        }
        if (!((user.getRole() == Role.FACILITATOR && target == Role.MENTOR)
                || (user.getRole() == Role.MENTOR && target == Role.FACILITATOR))) {
            throw new BusinessRuleException("ROLE_CHANGE_NOT_ALLOWED",
                    "Only Facilitator and Mentor accounts can be switched without creating a new account.");
        }

        if (target == Role.MENTOR && activityRepository.countByFacilitatorUserId(userId) > 0) {
            throw new BusinessRuleException("ROLE_HAS_DEPENDENCIES",
                    "This facilitator still owns learning activities. Reassign them before switching roles.");
        }
        if (target == Role.FACILITATOR
                && (teamRepository.countByMentorUserId(userId) > 0 || projectReviewRepository.countByMentorUserId(userId) > 0)) {
            throw new BusinessRuleException("ROLE_HAS_DEPENDENCIES",
                    "This mentor is still assigned to teams or reviews. Reassign them before switching roles.");
        }

        entityManager.flush();
        if (user.getRole() == Role.FACILITATOR) {
            entityManager.createNativeQuery("delete from facilitator where facilitator_id = :id")
                    .setParameter("id", userId).executeUpdate();
            entityManager.createNativeQuery("insert into mentor (mentor_id, organisation, expertise) values (:id, null, null)")
                    .setParameter("id", userId).executeUpdate();
        } else {
            entityManager.createNativeQuery("delete from mentor where mentor_id = :id")
                    .setParameter("id", userId).executeUpdate();
            entityManager.createNativeQuery("insert into facilitator (facilitator_id, specialisation) values (:id, null)")
                    .setParameter("id", userId).executeUpdate();
        }
        entityManager.createNativeQuery("update users set role = :role where user_id = :id")
                .setParameter("role", target.name()).setParameter("id", userId).executeUpdate();
        entityManager.clear();
        auditService.record(actor(), "USER_ROLE_CHANGED", "User", userId, "Role changed to " + target);
        return userMapper.toProfile(find(userId));
    }

    private User find(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found."));
    }

    private User actor() {
        return userRepository.findById(securityUtils.currentUserId()).orElse(null);
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
}
