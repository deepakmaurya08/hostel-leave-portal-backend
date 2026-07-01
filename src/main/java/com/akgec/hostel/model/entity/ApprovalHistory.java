package com.akgec.hostel.model.entity;

import com.akgec.hostel.model.enums.ApprovalAction;
import com.akgec.hostel.model.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "approval_history")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_id", nullable = false)
    private LeaveRequest leaveRequest;

    @Column(name = "performed_by")
    private String performedBy; // username or "PARENT" for parent

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private ApprovalAction action;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "ip_address")
    private String ipAddress;

    @CreatedDate
    @Column(name = "timestamp", updatable = false)
    private LocalDateTime timestamp;
}
