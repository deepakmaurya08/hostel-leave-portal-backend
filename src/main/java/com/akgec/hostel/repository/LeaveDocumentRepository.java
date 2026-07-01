package com.akgec.hostel.repository;

import com.akgec.hostel.model.entity.LeaveDocuments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface LeaveDocumentRepository
        extends JpaRepository<LeaveDocuments, Long> {
    List<LeaveDocuments> findByLeaveRequestId(Long leaveId);
}