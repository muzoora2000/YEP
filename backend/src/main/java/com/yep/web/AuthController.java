package com.yep.web;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
import com.yep.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        AuthResponse response = authService.login(request, clientIp(servletRequest));
        servletResponse.addHeader(HttpHeaders.SET_COOKIE,
                authCookie(response.token(), response.expiresIn(), isSecure(servletRequest)).toString());
        return response;
    }

    @PostMapping("/logout")
    public MessageResponse logout(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        servletResponse.addHeader(HttpHeaders.SET_COOKIE,
                authCookie("", 0, isSecure(servletRequest)).toString());
        return authService.logout();
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return authService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return authService.resetPassword(request);
    }

    @GetMapping("/me")
    public UserProfile me() {
        return authService.me();
    }

    @PutMapping("/me")
    public UserProfile updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return authService.updateProfile(request);
    }

    @PutMapping("/me/password")
    public MessageResponse changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return authService.changePassword(request);
    }

    @PutMapping("/me/consent")
    public UserProfile updateConsent(@RequestBody ConsentRequest request) {
        return authService.updateConsent(request);
    }

    private ResponseCookie authCookie(String token, long maxAgeSeconds, boolean secure) {
        return ResponseCookie.from("yep_token", token)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
    }

    private boolean isSecure(HttpServletRequest request) {
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        return request.isSecure() || (forwardedProto != null && forwardedProto.equalsIgnoreCase("https"));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
