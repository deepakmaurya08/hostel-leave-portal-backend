package com.akgec.hostel.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

public class AuthRequest {

    @Data
    public static class LoginRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data
    public static class RegisterStudentRequest {
        @NotBlank private String name;
        @NotBlank @Email private String email;
        @NotBlank private String password;
        @NotBlank private String studentNo;
        @NotBlank private String rollNumber;
        @NotBlank private String courseBranch;
        private Integer year;
        @NotBlank private String hostelName;
        @NotBlank private String roomNumber;
        @NotBlank @Email private String parentEmail;
        @NotBlank private String parentPhone;
        private String mobileNumber;
        private String homeAddress;
        private String emergencyContact;
    }
}
