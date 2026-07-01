package com.akgec.hostel.controller;

import com.akgec.hostel.dto.response.ApiResponse;
import com.akgec.hostel.dto.response.LeaveResponse;
import com.akgec.hostel.exception.HostelException;
import com.akgec.hostel.model.entity.ContactMessage;
import com.akgec.hostel.model.entity.Student;
import com.akgec.hostel.model.entity.User;
import com.akgec.hostel.model.enums.Role;
import com.akgec.hostel.repository.ContactMessageRepository;
import com.akgec.hostel.repository.LeaveRequestRepository;
import com.akgec.hostel.repository.StudentRepository;
import com.akgec.hostel.repository.UserRepository;
import com.akgec.hostel.service.impl.AdminService;
import com.akgec.hostel.service.impl.LeaveService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService           adminService;
    private final UserRepository         userRepository;
    private final StudentRepository      studentRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveService           leaveService;
    private final ContactMessageRepository contactMessageRepository;

    // ── User Management ────────────────────────────────────────────────────────

    @PostMapping("/users")
    public ResponseEntity<ApiResponse<User>> createUser(@RequestBody CreateUserRequest req) {
        User user = adminService.createStaffUser(req.getName(), req.getEmail(), req.getPassword(), req.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(user, "User created"));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getAllUsers()));
    }

    @GetMapping("/users/role/{role}")
    public ResponseEntity<ApiResponse<List<User>>> getUsersByRole(@PathVariable Role role) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getUsersByRole(role)));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<ApiResponse<User>> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new HostelException.ResourceNotFoundException("User", "id", id));
        if (req.getName()  != null) user.setName(req.getName());
        if (req.getEmail() != null) user.setEmail(req.getEmail());
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success(user, "User updated"));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userRepository.findById(id).orElseThrow(() -> new HostelException.ResourceNotFoundException("User", "id", id));
        userRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success(null, "User deleted"));
    }

    @PatchMapping("/users/{id}/toggle-active")
    public ResponseEntity<ApiResponse<User>> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminService.toggleUserActive(id)));
    }

    @PatchMapping("/users/{id}/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@PathVariable Long id, @RequestBody ResetPwdRequest req) {
        adminService.resetPassword(id, req.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success(null, "Password reset"));
    }

    // ── Student Management ─────────────────────────────────────────────────────

    @GetMapping("/students")
    public ResponseEntity<ApiResponse<List<Student>>> getAllStudents() {
        return ResponseEntity.ok(ApiResponse.success(studentRepository.findAll()));
    }

    @PostMapping("/students")
    public ResponseEntity<ApiResponse<User>> createStudent(@RequestBody CreateStudentRequest req) {
        // Create user account
        User user = adminService.createStaffUser(req.getName(), req.getEmail(), req.getPassword(), Role.ROLE_STUDENT);
        // Create student profile
        Student s = Student.builder()
                .user(user)
                .studentNo(req.getStudentNo())
                .rollNumber(req.getRollNumber())
                .courseBranch(req.getCourseBranch())
                .year(req.getYear())
                .hostelName(req.getHostelName())
                .roomNumber(req.getRoomNumber())
                .parentEmail(req.getParentEmail())
                .parentPhone(req.getParentPhone())
                .mobileNumber(req.getMobileNumber())
                .homeAddress(req.getHomeAddress())
                .attendancePercentage(req.getAttendancePercentage())
                .build();
        studentRepository.save(s);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(user, "Student created"));
    }

    @PutMapping("/students/{id}")
    public ResponseEntity<ApiResponse<Student>> updateStudent(@PathVariable Long id, @RequestBody UpdateStudentRequest req) {
        Student s = studentRepository.findById(id)
                .orElseThrow(() -> new HostelException.ResourceNotFoundException("Student", "id", id));
        if (req.getCourseBranch() != null) s.setCourseBranch(req.getCourseBranch());
        if (req.getHostelName()   != null) s.setHostelName(req.getHostelName());
        if (req.getRoomNumber()   != null) s.setRoomNumber(req.getRoomNumber());
        if (req.getParentEmail()  != null) s.setParentEmail(req.getParentEmail());
        if (req.getParentPhone()  != null) s.setParentPhone(req.getParentPhone());
        if (req.getMobileNumber() != null) s.setMobileNumber(req.getMobileNumber());
        if (req.getYear()         != null) s.setYear(req.getYear());
        studentRepository.save(s);
        return ResponseEntity.ok(ApiResponse.success(s, "Student updated"));
    }

    @DeleteMapping("/students/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteStudent(@PathVariable Long id) {
        studentRepository.findById(id).orElseThrow(() -> new HostelException.ResourceNotFoundException("Student", "id", id));
        studentRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Student deleted"));
    }

    // ── Leave Management (admin sees ALL) ─────────────────────────────────────

    @GetMapping("/leaves")
    public ResponseEntity<ApiResponse<List<LeaveResponse>>> getAllLeaves() {
        List<LeaveResponse> leaves = leaveRequestRepository.findAll().stream()
                .map(l -> leaveService.buildLeaveResponse(l, false))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(leaves));
    }

    @GetMapping("/leaves/{id}")
    public ResponseEntity<ApiResponse<LeaveResponse>> getLeave(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(leaveService.getLeaveById(id)));
    }

    // ── Reports & Audit ────────────────────────────────────────────────────────

    @GetMapping("/reports/system")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReport() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getSystemReport()));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAudit(@RequestParam(defaultValue = "200") int limit) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getAuditLogs(limit)));
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────
    @Data public static class CreateUserRequest    { private String name, email, password; private Role role; }
    @Data public static class UpdateUserRequest    { private String name, email; }
    @Data public static class ResetPwdRequest      { private String newPassword; }
    @Data public static class CreateStudentRequest {
        private String name, email, password, studentNo, rollNumber, courseBranch;
        private String hostelName, roomNumber, parentEmail, parentPhone, mobileNumber, homeAddress;
        private Integer year;
        private Double attendancePercentage;
    }
    @Data public static class UpdateStudentRequest {
        private String courseBranch, hostelName, roomNumber, parentEmail, parentPhone, mobileNumber;
        private Integer year;
        private Double attendancePercentage;
    }

    //--------------------------------------

    @GetMapping("/messages")
    public ResponseEntity<ApiResponse<List<ContactMessage>>> getMessages() {
        return ResponseEntity.ok(ApiResponse.success(
                contactMessageRepository.findAllByOrderByCreatedAtDesc()));
    }

    @PatchMapping("/messages/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable Long id) {
        contactMessageRepository.findById(id).ifPresent(m -> {
            m.setRead(true);
            contactMessageRepository.save(m);
        });
        return ResponseEntity.ok(ApiResponse.success(null, "Marked as read"));
    }
}
