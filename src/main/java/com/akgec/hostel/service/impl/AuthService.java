package com.akgec.hostel.service.impl;

import com.akgec.hostel.dto.request.AuthRequest;
import com.akgec.hostel.dto.response.AuthResponse;
import com.akgec.hostel.exception.HostelException;
import com.akgec.hostel.model.entity.ContactMessage;
import com.akgec.hostel.model.entity.User;
import com.akgec.hostel.model.enums.Role;
import com.akgec.hostel.repository.ContactMessageRepository;
import com.akgec.hostel.repository.UserRepository;
import com.akgec.hostel.security.CustomUserDetails;
import com.akgec.hostel.security.JwtTokenProvider;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider      jwtTokenProvider;
    private final UserRepository        userRepository;
    private final PasswordEncoder       passwordEncoder;
    private final JavaMailSender        mailSender;
    private final ContactMessageRepository contactMessageRepository;

    @Value("${spring.mail.from}")
    private String fromEmail;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.admin.email:admin@akgec.ac.in}")
    private String adminEmail;

    // In-memory token store (replace with DB table in production)
    private final Map<String, ResetTokenEntry> resetTokens = new ConcurrentHashMap<>();

    // ── Login ──────────────────────────────────────────────────────────────────
    public AuthResponse login(AuthRequest.LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        String token = jwtTokenProvider.generateToken(auth);
        CustomUserDetails u = (CustomUserDetails) auth.getPrincipal();
        log.info("Login: {}", u.getEmail());
        return AuthResponse.builder()
                .token(token).tokenType("Bearer")
                .role(u.getAuthorities().iterator().next().getAuthority())
                .userId(u.getId()).name(u.getName()).email(u.getEmail())
                .profilePhoto(u.getProfilePhoto())
                .build();
    }

    // ── Forgot password ────────────────────────────────────────────────────────
    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString().replace("-", "");
            resetTokens.put(token, new ResetTokenEntry(email, LocalDateTime.now().plusHours(2)));
            String resetUrl = frontendUrl + "/reset-password?token=" + token;
            try {
                MimeMessage msg = mailSender.createMimeMessage();
                MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
                h.setFrom(fromEmail); h.setTo(email);
                h.setSubject("Password Reset — AKGEC Hostel Portal");
                h.setText("""
                    <!DOCTYPE html><html><body style='font-family:Arial,sans-serif;max-width:520px;margin:auto'>
                    <div style='background:#0A1628;padding:20px;text-align:center'>
                      <h2 style='color:white;margin:0'>AKGEC Hostel Portal</h2>
                    </div>
                    <div style='padding:28px;border:1px solid #e0e0e0'>
                      <p>Hello <b>%s</b>,</p>
                      <p>We received a request to reset your password.</p>
                      <div style='text-align:center;margin:28px 0'>
                        <a href='%s' style='background:#4F46E5;color:white;padding:14px 32px;
                           text-decoration:none;border-radius:8px;font-weight:bold'>
                          Reset Password
                        </a>
                      </div>
                      <p style='color:#888;font-size:12px'>This link expires in 2 hours.
                      If you did not request this, ignore this email.</p>
                    </div></body></html>
                    """.formatted(user.getName(), resetUrl), true);
                mailSender.send(msg);
                log.info("Password reset email sent to {}", email);
            } catch (Exception e) {
                log.error("Failed to send reset email: {}", e.getMessage());
            }
        });
    }

    // ── Reset password by token ────────────────────────────────────────────────
    @Transactional
    public void resetPasswordByToken(String token, String newPassword) {
        ResetTokenEntry entry = resetTokens.get(token);
        if (entry == null || entry.expiry().isBefore(LocalDateTime.now()))
            throw new HostelException.TokenExpiredException("Reset link has expired or is invalid");
        if (newPassword == null || newPassword.length() < 8)
            throw new HostelException.InvalidRequestException("Password must be at least 8 characters");

        User user = userRepository.findByEmail(entry.email())
                .orElseThrow(() -> new HostelException.ResourceNotFoundException("User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        resetTokens.remove(token);
        log.info("Password reset for {}", entry.email());
    }

    // ── Contact admin ──────────────────────────────────────────────────────────
    public void contactAdmin(String name, String email, String message) {
        contactMessageRepository.save(ContactMessage.builder()
                .name(name)
                .email(email)
                .message(message)
                .isRead(false)
                .build());
        log.info("Contact message saved from {} ({})", name, email);
    }

    private record ResetTokenEntry(String email, LocalDateTime expiry) {}
}
