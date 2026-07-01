package com.akgec.hostel.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "leave_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveDocuments {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_request_id")
    private LeaveRequest leaveRequest;

    private String fileName;

    private String contentType;

    @Column(name = "data", columnDefinition = "bytea")
    private byte[] data;
}
