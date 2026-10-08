package com.yep.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yep.domain.enums.AccountStatus;
import com.yep.domain.enums.Role;
import com.yep.domain.user.Admin;
import com.yep.domain.user.Facilitator;
import com.yep.domain.user.Mentor;
import com.yep.domain.user.Participant;
import com.yep.domain.user.User;
import com.yep.dto.PageResponse;
import com.yep.dto.UserDtos;
import com.yep.exception.BadRequestException;
import com.yep.exception.ForbiddenException;
import com.yep.exception.NotFoundException;
import com.yep.repository.UserRepository;
import com.yep.security.AuthenticatedUser;
import com.yep.security.SecurityUtils;

/**
 * User administration. Reads are polymorphic: {@link UserDtos.UserResponse#of}
 * projects each concrete subclass differently while the service only ever deals
 * with the {@link User} abstraction.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final AuthService authService;

    public UserService(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public UserDtos.UserResponse getById(Long id) {
        AuthenticatedUser principal = SecurityUtils.current();
        if (!principal.id().equals(id) && !principal.isAdmin()) {
            throw new ForbiddenException("You may only view your own profile");
        }
        return UserDtos.UserResponse.of(find(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<UserDtos.UserResponse> list(Role role, String query, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by("createdAt").descending());
        Page<User> result;
        String q = query == null || query.isBlank() ? null : query.trim();
        if (role != null && q != null) {
            result = userRepository.searchByRole(role.name(), q, pageable);
        } else if (role != null) {
            result = userRepository.findByRole(role, pageable);
        } else if (q != null) {
            result = userRepository.search(q, pageable);
        } else {
            result = userRepository.findAll(pageable);
        }
        return PageResponse.from(result.map(UserDtos.UserResponse::of));
    }

    @Transactional
    public UserDtos.UserResponse updateProfile(Long id, UserDtos.UpdateProfileRequest request) {
        AuthenticatedUser principal = SecurityUtils.current();
        if (!principal.id().equals(id) && !principal.isAdmin()) {
            throw new ForbiddenException("You may only edit your own profile");
        }
        User user = find(id);

        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName().trim());
        }
        if (request.country() != null) {
            user.setCountry(request.country());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }

        if (user instanceof Participant participant) {
            if (request.age() != null) {
                if (request.age() < 16 || request.age() > 40) {
                    throw new BadRequestException("Age must be between 16 and 40");
                }
                participant.setAge(request.age());
            }
            if (request.skillLevel() != null) {
                participant.setSkillLevel(request.skillLevel());
            }
        } else if (user instanceof Facilitator facilitator) {
            if (request.expertiseArea() != null) {
                facilitator.setExpertiseArea(request.expertiseArea());
            }
            if (request.expertiseTopics() != null) {
                facilitator.setExpertiseTopics(request.expertiseTopics());
            }
        } else if (user instanceof Mentor mentor) {
            if (request.mentorshipAreas() != null) {
                mentor.setMentorshipAreas(request.mentorshipAreas());
            }
        }

        return UserDtos.UserResponse.of(userRepository.save(user));
    }

    @Transactional
    public UserDtos.UserResponse adminUpdate(Long id, UserDtos.AdminUpdateUserRequest request) {
        requireAdmin();
        User user = find(id);
        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName().trim());
        }
        if (request.accountStatus() != null) {
            if (user.getId().equals(SecurityUtils.currentUserId()) && request.accountStatus() != AccountStatus.ACTIVE) {
                throw new BadRequestException("You cannot deactivate your own account");
            }
            user.setAccountStatus(request.accountStatus());
        }
        if (request.permissions() != null && user instanceof Admin admin) {
            admin.setPermissions(request.permissions());
        }
        return UserDtos.UserResponse.of(userRepository.save(user));
    }

    /** Administrators can mint any role, including other administrators. */
    @Transactional
    public UserDtos.UserResponse adminCreateUser(Role role, String fullName, String email, String password) {
        requireAdmin();
        if (email == null || userRepository.existsByEmail(email.toLowerCase())) {
            throw new BadRequestException("Email is already in use");
        }
        User user = authService.createRawUser(role, fullName, email, password);
        return UserDtos.UserResponse.of(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public long countByRole(Role role) {
        return userRepository.countByRole(role);
    }

    private User find(Long id) {
        return userRepository.findById(id).orElseThrow(() -> NotFoundException.resource("User", id));
    }

    private void requireAdmin() {
        if (!SecurityUtils.current().isAdmin()) {
            throw new ForbiddenException("Administrator role required");
        }
    }
}
