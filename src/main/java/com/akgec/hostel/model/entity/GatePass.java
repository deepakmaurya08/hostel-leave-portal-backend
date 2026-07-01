package com.akgec.hostel.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "gate_passes")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GatePass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_id", nullable = false, unique = true)
    private LeaveRequest leaveRequest;

    // Path to generated PDF
    @Column(name = "pdf_path")
    private String pdfPath;

    // Encrypted QR token: LeaveID|StudentID|token
    @Column(name = "qr_token", nullable = false, unique = true)
    private String qrToken;

    // Path to QR code image
    @Column(name = "qr_image_path")
    private String qrImagePath;

    // Security guard who marked exit
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exit_marked_by")
    private User exitMarkedBy;

    // Security guard who marked entry
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_marked_by")
    private User entryMarkedBy;

    @Column(name = "exit_time")
    private LocalDateTime exitTime;

    @Column(name = "entry_time")
    private LocalDateTime entryTime;

    @Column(name = "exit_remarks")
    private String exitRemarks;

    @Column(name = "entry_remarks")
    private String entryRemarks;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public boolean hasExited() {
        return exitTime != null;
    }

    public boolean hasReturned() {
        return entryTime != null;
    }
}
