package com.akgec.hostel.controller;

import com.akgec.hostel.dto.response.ApiResponse;
import com.akgec.hostel.exception.HostelException;
import com.akgec.hostel.model.entity.Student;
import com.akgec.hostel.model.entity.User;
import com.akgec.hostel.repository.StudentRepository;
import com.akgec.hostel.repository.UserRepository;
import com.akgec.hostel.util.SecurityUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository    userRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder   passwordEncoder;
    private final SecurityUtils     securityUtils;

    @Value("${app.file.upload-dir}")
    private String uploadDir;

    /** GET /api/profile/me — returns full profile for any role */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyProfile() {
        Long userId = securityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new HostelException.ResourceNotFoundException("User not found"));

        Map<String, Object> profile = new HashMap<>();
        profile.put("id",    user.getId());
        profile.put("name",  user.getName());
        profile.put("email", user.getEmail());
        profile.put("role",  user.getRole().name());
        profile.put("photo", user.getProfilePhoto());

        studentRepository.findByUserId(userId).ifPresent(s -> {
            Map<String, Object> sd = new HashMap<>();
            sd.put("id",               s.getId());
            sd.put("studentNo",        s.getStudentNo());
            sd.put("rollNumber",       s.getRollNumber());
            sd.put("courseBranch",     s.getCourseBranch());
            sd.put("year",             s.getYear());
            sd.put("hostelName",       s.getHostelName());
            sd.put("roomNumber",       s.getRoomNumber());
            sd.put("parentEmail",      s.getParentEmail());
            sd.put("parentPhone",      s.getParentPhone());
            sd.put("mobileNumber",     s.getMobileNumber());
            sd.put("homeAddress",      s.getHomeAddress());
            sd.put("emergencyContact", s.getEmergencyContact());
            profile.put("student", sd);
        });
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    /** GET /api/profile/attendance — auto-fill attendance from backend */
    @GetMapping("/attendance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAttendance() {
        Long userId = securityUtils.getCurrentUserId();
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new HostelException.ResourceNotFoundException("Student not found"));
        Map<String, Object> data = new HashMap<>();
        data.put("percentage", student.getAttendancePercentage() != null ? student.getAttendancePercentage() : 0.0);
        data.put("source", "College ERP");
        return ResponseEntity.ok(ApiResponse.success(data));
    }
    /** PATCH /api/profile/update-contact */
    @PatchMapping("/update-contact")
    public ResponseEntity<ApiResponse<Void>> updateContact(@RequestBody UpdateContactRequest req) {
        Long userId = securityUtils.getCurrentUserId();
        studentRepository.findByUserId(userId).ifPresent(s -> {
            if (req.getMobileNumber() != null) s.setMobileNumber(req.getMobileNumber());
            if (req.getParentPhone()  != null) s.setParentPhone(req.getParentPhone());
            studentRepository.save(s);
        });
        return ResponseEntity.ok(ApiResponse.success(null, "Updated"));
    }

    /** POST /api/profile/upload-photo — for ALL roles */
    @PostMapping("/upload-photo")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadPhoto(
            @RequestParam("photo") MultipartFile photo) throws IOException {
        Long userId = securityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new HostelException.ResourceNotFoundException("User not found"));

        String ext      = photo.getOriginalFilename() != null
                ? photo.getOriginalFilename().substring(photo.getOriginalFilename().lastIndexOf('.'))
                : ".jpg";
        String fileName = "photo_" + userId + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
        Path dir        = Paths.get(uploadDir, "photos");
        Files.createDirectories(dir);
        Path filePath   = dir.resolve(fileName);
        Files.copy(photo.getInputStream(), filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        String photoUrl = "/uploads/photos/" + fileName;
        user.setProfilePhoto(photoUrl);
        userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.success(Map.of("photoUrl", photoUrl)));
    }

    @Data public static class UpdateContactRequest { private String mobileNumber; private String parentPhone; }
}
