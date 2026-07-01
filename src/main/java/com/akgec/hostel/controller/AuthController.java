package com.akgec.hostel.controller;

import com.akgec.hostel.dto.request.AuthRequest;
import com.akgec.hostel.dto.response.ApiResponse;
import com.akgec.hostel.dto.response.AuthResponse;
import com.akgec.hostel.exception.HostelException;
import com.akgec.hostel.model.entity.User;
import com.akgec.hostel.model.enums.Role;
import com.akgec.hostel.repository.UserRepository;
import com.akgec.hostel.service.impl.AuthService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService     authService;
    private final UserRepository  userRepository;

    @Value("${app.admin.name:Portal Administrator}")
    private String adminName;

    @Value("${app.admin.description:Developer of AKGEC Hostel Leave Portal, Deepak Maurya}")
    private String adminDescription;

    @Value("${app.admin.batch:B.Tech IT — 2027 Batch, AKGEC Ghaziabad}")
    private String adminBatch;

    /**
     * POST /api/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody AuthRequest.LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request), "Login successful"));
    }

    /**
     * POST /api/auth/forgot-password
     * Sends a password reset email
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestBody ForgotPasswordRequest req) {
        authService.forgotPassword(req.getEmail());
        // Always return 200 to not reveal if email exists
        return ResponseEntity.ok(ApiResponse.success(null, "If that email is registered, a reset link has been sent"));
    }

    /**
     * POST /api/auth/reset-password
     * Resets password using the token from email
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody ResetPasswordRequest req) {
        authService.resetPasswordByToken(req.getToken(), req.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success(null, "Password reset successfully"));
    }

    /**
     * POST /api/auth/contact-admin
     * Anyone can send a message to admin (pre-login)
     */
    @PostMapping("/contact-admin")
    public ResponseEntity<ApiResponse<Void>> contactAdmin(@RequestBody ContactAdminRequest req) {
        authService.contactAdmin(req.getName(), req.getEmail(), req.getMessage());
        return ResponseEntity.ok(ApiResponse.success(null, "Message sent to admin"));
    }

    /**
     * GET /api/auth/admin-info
     * Returns admin's public profile for the contact card on login page
     */
    @GetMapping("/admin-info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAdminInfo() {
        Map<String, Object> info = new HashMap<>();
        // Find the first active admin user
        List<User> admins = userRepository.findByRoleAndActive(Role.ROLE_ADMIN, true);
        if (!admins.isEmpty()) {
            User admin = admins.get(0);
            info.put("name",        admin.getName());
            info.put("email",       admin.getEmail());
            info.put("photo",       admin.getProfilePhoto());
        } else {
            info.put("name",  adminName);
            info.put("email", "admin@akgec.ac.in");
            info.put("photo", null);
        }
        info.put("description", adminDescription);
        info.put("batch",       adminBatch);
        return ResponseEntity.ok(ApiResponse.success(info));
    }

    // ── Inner DTOs ─────────────────────────────────────────────────────────────
    @Data public static class ForgotPasswordRequest { private String email; }
    @Data public static class ResetPasswordRequest  { private String token; private String newPassword; }
    @Data public static class ContactAdminRequest   { private String name; private String email; private String message; }
}
