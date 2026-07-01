package com.akgec.hostel.service.impl;

import com.akgec.hostel.dto.response.ApiResponse;
import com.akgec.hostel.exception.HostelException;
import com.akgec.hostel.model.entity.Student;
import com.akgec.hostel.model.entity.User;
import com.akgec.hostel.model.enums.LeaveStatus;
import com.akgec.hostel.model.enums.Role;
import com.akgec.hostel.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final ApprovalHistoryRepository approvalHistoryRepository;
    private final GatePassRepository gatePassRepository;
    private final PasswordEncoder passwordEncoder;

    // ─────────────────────────────────────────────────────────────────────────
    // User Management
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public User createStaffUser(String name, String email, String password, Role role) {
        if (userRepository.existsByEmail(email)) {
            throw new HostelException.DuplicateResourceException("Email already registered: " + email);
        }


        User user = User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(role)
                .active(true)
                .build();
        user = userRepository.save(user);
        log.info("Staff user created: {} ({})", name, role);
        return user;
    }

    @Transactional
    public User toggleUserActive(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new HostelException.ResourceNotFoundException("User", "id", userId));
        user.setActive(!user.isActive());
        user = userRepository.save(user);
        log.info("User {} active status toggled to {}", user.getEmail(), user.isActive());
        return user;
    }

    @Transactional
    public User resetPassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new HostelException.ResourceNotFoundException("User", "id", userId));
        user.setPassword(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getUsersByRole(Role role) {
        return userRepository.findByRole(role);
    }

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Reports
    // ─────────────────────────────────────────────────────────────────────────
    public Map<String, Object> getSystemReport() {
        Map<String, Object> report = new HashMap<>();

        // Leave counts by status
        Map<String, Long> leaveCounts = new HashMap<>();
        for (LeaveStatus status : LeaveStatus.values()) {
            leaveCounts.put(status.name(), leaveRequestRepository.countByStatus(status));
        }
        report.put("leaveCountsByStatus", leaveCounts);

        // User counts
        Map<String, Long> userCounts = new HashMap<>();
        for (Role role : Role.values()) {
            userCounts.put(role.name(), (long) userRepository.findByRole(role).size());
        }
        report.put("userCountsByRole", userCounts);

        // Total leaves
        report.put("totalLeaves", leaveRequestRepository.count());
        report.put("totalStudents", studentRepository.count());

        // Recent activity (last 30 days)
        report.put("recentLeaves",
                leaveRequestRepository.findLeavesSince(LocalDateTime.now().minusDays(30)).size());

        // Currently on leave
        // Count students whose approved leave period includes today
        long onLeaveNow = leaveRequestRepository.findAll().stream()
                .filter(l -> l.getStatus() == com.akgec.hostel.model.enums.LeaveStatus.APPROVED
                        || l.getStatus() == com.akgec.hostel.model.enums.LeaveStatus.COMPLETED)
                .filter(l -> l.getFromDate() != null && l.getToDate() != null)
                .filter(l -> {
                    java.time.LocalDate today = java.time.LocalDate.now();
                    return !today.isBefore(l.getFromDate()) && !today.isAfter(l.getToDate());
                })
                .count();
        report.put("studentsCurrentlyOnLeave", onLeaveNow);

        return report;
    }

    public Map<String, Object> getAuditLogs(int limit) {
        Map<String, Object> logs = new HashMap<>();
        logs.put("recentActions", approvalHistoryRepository.findAll()
                .stream()
                .filter(a -> a.getTimestamp() != null)  // ← skip null timestamps
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(limit)
                .map(a -> {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("id",          a.getId());
                    entry.put("performedBy", a.getPerformedBy());
                    entry.put("role",        a.getRole() != null ? a.getRole().name() : null);
                    entry.put("action",      a.getAction() != null ? a.getAction().name() : null);
                    entry.put("remarks",     a.getRemarks());
                    entry.put("timestamp",   a.getTimestamp());
                    return entry;
                })
                .toList());
        return logs;
    }
}
