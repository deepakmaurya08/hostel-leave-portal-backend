package com.akgec.hostel.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "students")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // From AKGEC physical form - Student No.
    @Column(name = "student_no", nullable = false, unique = true)
    private String studentNo;

    // Roll Number (internal)
    @Column(name = "roll_number", nullable = false, unique = true)
    private String rollNumber;

    // Course/Branch from form field 2
    @Column(name = "course_branch", nullable = false)
    private String courseBranch;

    // Year from form field 2
    @Column(name = "year")
    private Integer year;

    // Hostel Name from form field 3
    @Column(name = "hostel_name", nullable = false)
    private String hostelName;

    // Room No. from form field 3
    @Column(name = "room_number", nullable = false)
    private String roomNumber;

    // Parent/Guardian info
    @Column(name = "parent_email", nullable = false)
    private String parentEmail;

    // Parent's Mobile No. from form field 9
    @Column(name = "parent_phone", nullable = false)
    private String parentPhone;

    // Applicant's Mobile No. from form field 8
    @Column(name = "mobile_number")
    private String mobileNumber;

    // Address of student
    @Column(name = "home_address", columnDefinition = "TEXT")
    private String homeAddress;

    @Column(name = "emergency_contact")
    private String emergencyContact;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;


    @Column(name = "attendance_percentage")
    private Double attendancePercentage;
}
