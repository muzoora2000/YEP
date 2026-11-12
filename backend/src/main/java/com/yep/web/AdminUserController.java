package com.yep.web;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yep.domain.enums.AccountStatus;
import com.yep.domain.enums.Role;
import com.yep.dto.AdminDtos.ApprovalRequest;
import com.yep.dto.AdminDtos.CreateUserRequest;
import com.yep.dto.AdminDtos.RoleChangeRequest;
import com.yep.dto.AdminDtos.StatusChangeRequest;
import com.yep.dto.AuthDtos.UserProfile;
import com.yep.dto.PageResponse;
import com.yep.service.AdminUserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public PageResponse<UserProfile> list(@RequestParam(required = false) Role role,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return PageResponse.from(adminUserService.list(role, status, q, pageable));
    }

    @GetMapping("/{userId}")
    public UserProfile get(@PathVariable Long userId) {
        return adminUserService.get(userId);
    }

    @PostMapping
    public ResponseEntity<UserProfile> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminUserService.create(request));
    }

    @PutMapping("/{userId}/status")
    public UserProfile changeStatus(@PathVariable Long userId, @Valid @RequestBody StatusChangeRequest request) {
        return adminUserService.changeStatus(userId, request);
    }

    @PutMapping("/{userId}/approve")
    public UserProfile approve(@PathVariable Long userId,
            @RequestBody(required = false) ApprovalRequest request) {
        boolean approved = request == null || request.approved();
        return adminUserService.approve(userId, approved);
    }

    @PutMapping("/{userId}/role")
    public UserProfile changeRole(@PathVariable Long userId, @Valid @RequestBody RoleChangeRequest request) {
        return adminUserService.changeRole(userId, request);
    }
}
