package com.yep.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yep.domain.enums.Role;
import com.yep.dto.PageResponse;
import com.yep.dto.UserDtos;
import com.yep.service.UserService;

import jakarta.validation.Valid;

/** User administration - {@code /api/v1/users}. */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public UserDtos.UserResponse get(@PathVariable Long id) {
        return userService.getById(id);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<UserDtos.UserResponse> list(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) String q,
            Pageable pageable) {
        return userService.list(role, q, pageable.getPageNumber(), pageable.getPageSize());
    }

    @PatchMapping("/{id}")
    public UserDtos.UserResponse updateProfile(@PathVariable Long id,
            @Valid @RequestBody UserDtos.UpdateProfileRequest request) {
        return userService.updateProfile(id, request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserDtos.UserResponse adminUpdate(@PathVariable Long id,
            @Valid @RequestBody UserDtos.AdminUpdateUserRequest request) {
        return userService.adminUpdate(id, request);
    }

    /** Administrators mint accounts of any role (including other admins). */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDtos.UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.adminCreateUser(request.role(), request.fullName(), request.email(),
                        request.password()));
    }

    public record CreateUserRequest(
            @jakarta.validation.constraints.NotBlank String fullName,
            @jakarta.validation.constraints.Email String email,
            @jakarta.validation.constraints.Size(min = 8, max = 72) String password,
            Role role) {
    }
}
