package com.akgec.hostel.model.entity;

import com.akgec.hostel.model.enums.LeaveStatus;
import com.akgec.hostel.model.enums.LeaveType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "leave_requests")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Leave Pass Number e.g. LV-2026-0012
    @Column(name = "leave_pass_number", unique = true)
    private String leavePassNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false)
    private LeaveType leaveType;

    // Reason for Leave (State Clearly) - form field 5
    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    // Period of Leave - From Date - form field 4
    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    // Period of Leave - To Date - form field 4
    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    // Time Out - from form field 4
    @Column(name = "time_out")
    private LocalTime timeOut;

    // Destination / Name of person being visited - form field 6
    @Column(name = "destination")
    private String destination;

    // Name of the Person being visited - form field 6
    @Column(name = "visit_person_name")
    private String visitPersonName;

    // Relation with person being visited - form field 6
    @Column(name = "visit_person_relation")
    private String visitPersonRelation;

    // Address and Contact No. of person being visited - form field 7
    @Column(name = "visit_person_address", columnDefinition = "TEXT")
    private String visitPersonAddress;

    @Column(name = "visit_person_contact")
    private String visitPersonContact;

    // Emergency contact (Applicant's contact override)
    @Column(name = "emergency_contact")
    private String emergencyContact;

    // Attendance Percentage - form field (Attendance Verification by Department)
    @Column(name = "attendance_percentage")
    private Double attendancePercentage;

    // HOD/Co-ordinator signature info
    @Column(name = "hod_name")
    private String hodName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private LeaveStatus status = LeaveStatus.PENDING_PARENT;

    // Remarks from each level
    @Column(name = "warden_remarks", columnDefinition = "TEXT")
    private String wardenRemarks;

    @Column(name = "dean_remarks", columnDefinition = "TEXT")
    private String deanRemarks;

    // Warden's Remarks - No. of working days - form table
    @Column(name = "working_days_count")
    private Integer workingDaysCount;

    // Warden communication with parents time
    @Column(name = "warden_parent_comm_time")
    private String wardenParentCommTime;

    // Document uploads (stored as comma-separated paths)
    @OneToMany(mappedBy = "leaveRequest",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    @Builder.Default
    private List<LeaveDocuments> documents = new ArrayList<>();

    // Approved by
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warden_id")
    private User approvedByWarden;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dean_id")
    private User approvedByDean;

    // Timestamps
    @Column(name = "parent_approved_at")
    private LocalDateTime parentApprovedAt;

    @Column(name = "warden_approved_at")
    private LocalDateTime wardenApprovedAt;

    @Column(name = "dean_approved_at")
    private LocalDateTime deanApprovedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Gate Pass relationship
    @OneToOne(mappedBy = "leaveRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private GatePass gatePass;

    // Approval history
    @OneToMany(mappedBy = "leaveRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ApprovalHistory> approvalHistories = new ArrayList<>();

    // Helper: total leave days
    public long getLeaveDays() {
        if (fromDate != null && toDate != null) {
            return fromDate.until(toDate).getDays() + 1;
        }
        return 0;
    }
}
