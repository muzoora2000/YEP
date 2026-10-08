package com.yep.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yep.domain.enums.Role;
import com.yep.domain.user.Admin;
import com.yep.domain.user.Facilitator;
import com.yep.domain.user.Mentor;
import com.yep.domain.user.Participant;
import com.yep.domain.user.User;
import com.yep.dto.AuthDtos;
import com.yep.dto.UserDtos;
import com.yep.exception.BadRequestException;
import com.yep.exception.ConflictException;
import com.yep.exception.NotFoundException;
import com.yep.repository.UserRepository;
import com.yep.security.AuthenticatedUser;
import com.yep.security.JwtService;
import com.yep.security.SecurityUtils;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Registration, login and session handling.
 * Passwords are stored only as BCrypt hashes; the JWT is handed to the browser
 * both as a bearer token and as an httpOnly cookie.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserDtos.UserResponse register(AuthDtos.RegisterRequest request) {
        if (!Boolean.TRUE.equals(request.consentGiven())) {
            throw new BadRequestException("You must accept the privacy consent to register");
        }
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        Role role = request.role();
        if (role == Role.ADMIN) {
            throw new BadRequestException("Administrator accounts are created by an existing administrator");
        }

        User user = switch (role) {
            case PARTICIPANT -> buildParticipant(request, email);
            case FACILITATOR -> buildFacilitator(request, email);
            case MENTOR -> buildMentor(request, email);
            case ADMIN -> throw new BadRequestException("Administrator accounts are created by an administrator");
        };
        user.setAccountStatus(com.yep.domain.enums.AccountStatus.ACTIVE);

        return UserDtos.UserResponse.of(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request, HttpServletResponse response) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password()));
        } catch (AuthenticationException ex) {
            throw new com.yep.exception.UnauthorizedException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new NotFoundException("User not found"));

        String token = jwtService.generateToken(user);
        jwtService.writeAuthCookie(response, token);
        return AuthDtos.AuthResponse.bearer(token, jwtService.expiresInSeconds(), UserDtos.UserResponse.of(user));
    }

    @Transactional(readOnly = true)
    public UserDtos.UserResponse me() {
        AuthenticatedUser principal = SecurityUtils.current();
        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> new NotFoundException("User not found"));
        return UserDtos.UserResponse.of(user);
    }

    public void logout(HttpServletResponse response) {
        jwtService.clearAuthCookie(response);
    }

    @Transactional
    public UserDtos.UserResponse changePassword(AuthDtos.ChangePasswordRequest request) {
        AuthenticatedUser principal = SecurityUtils.current();
        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        return UserDtos.UserResponse.of(userRepository.save(user));
    }

    // ---------------------------------------------------------------------

    private Participant buildParticipant(AuthDtos.RegisterRequest request, String email) {
        Participant participant = new Participant();
        participant.setRole(Role.PARTICIPANT);
        participant.setEmail(email);
        participant.setFullName(request.fullName().trim());
        participant.setPasswordHash(passwordEncoder.encode(request.password()));
        participant.setCountry(request.country());
        participant.setPhone(request.phone());
        participant.setConsentGiven(true);
        if (request.age() != null) {
            if (request.age() < 16 || request.age() > 40) {
                throw new BadRequestException("Age must be between 16 and 40");
            }
            participant.setAge(request.age());
        }
        participant.setSkillLevel(request.skillLevel() == null
                ? com.yep.domain.enums.SkillLevel.BEGINNER
                : request.skillLevel());
        return participant;
    }

    private Facilitator buildFacilitator(AuthDtos.RegisterRequest request, String email) {
        Facilitator facilitator = new Facilitator();
        facilitator.setRole(Role.FACILITATOR);
        facilitator.setEmail(email);
        facilitator.setFullName(request.fullName().trim());
        facilitator.setPasswordHash(passwordEncoder.encode(request.password()));
        facilitator.setCountry(request.country());
        facilitator.setPhone(request.phone());
        facilitator.setConsentGiven(true);
        facilitator.setExpertiseArea(request.expertiseArea());
        facilitator.setExpertiseTopics(request.expertiseTopics());
        return facilitator;
    }

    private Mentor buildMentor(AuthDtos.RegisterRequest request, String email) {
        Mentor mentor = new Mentor();
        mentor.setRole(Role.MENTOR);
        mentor.setEmail(email);
        mentor.setFullName(request.fullName().trim());
        mentor.setPasswordHash(passwordEncoder.encode(request.password()));
        mentor.setCountry(request.country());
        mentor.setPhone(request.phone());
        mentor.setConsentGiven(true);
        mentor.setMentorshipAreas(request.mentorshipAreas());
        return mentor;
    }

    /** Used by the seeder and by administrators creating an admin account. */
    public User createRawUser(Role role, String fullName, String email, String rawPassword) {
        User user = switch (role) {
            case PARTICIPANT -> new Participant();
            case FACILITATOR -> new Facilitator();
            case MENTOR -> new Mentor();
            case ADMIN -> new Admin();
        };
        user.setRole(role);
        user.setFullName(fullName);
        user.setEmail(email.toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setConsentGiven(true);
        user.setAccountStatus(com.yep.domain.enums.AccountStatus.ACTIVE);
        return user;
    }
}
