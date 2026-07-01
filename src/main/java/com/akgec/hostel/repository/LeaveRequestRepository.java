package com.akgec.hostel.repository;

import com.akgec.hostel.model.entity.LeaveRequest;
import com.akgec.hostel.model.enums.LeaveStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    // Student queries
    List<LeaveRequest> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    Page<LeaveRequest> findByStudentId(Long studentId, Pageable pageable);
    List<LeaveRequest> findByStudentIdAndStatus(Long studentId, LeaveStatus status);
    long countByStudentIdAndStatus(Long studentId, LeaveStatus status);

    // Status queries
    List<LeaveRequest> findByStatusOrderByCreatedAtAsc(LeaveStatus status);
    Page<LeaveRequest> findByStatus(LeaveStatus status, Pageable pageable);

    // Warden
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.status = 'PENDING_WARDEN' ORDER BY lr.createdAt ASC")
    List<LeaveRequest> findPendingWardenRequests();

    // Dean
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.status = 'PENDING_DEAN' ORDER BY lr.createdAt ASC")
    List<LeaveRequest> findPendingDeanRequests();

    // Find by leave pass number
    Optional<LeaveRequest> findByLeavePassNumber(String leavePassNumber);

    // Check active leave (student already on leave)
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.student.id = :studentId " +
           "AND lr.status IN ('APPROVED', 'PENDING_PARENT', 'PENDING_WARDEN', 'PENDING_DEAN') " +
           "AND lr.fromDate <= :toDate AND lr.toDate >= :fromDate")
    List<LeaveRequest> findOverlappingLeaves(@Param("studentId") Long studentId,
                                              @Param("fromDate") LocalDate fromDate,
                                              @Param("toDate") LocalDate toDate);

    // Admin reports
    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.createdAt >= :startDate ORDER BY lr.createdAt DESC")
    List<LeaveRequest> findLeavesSince(@Param("startDate") java.time.LocalDateTime startDate);

    // Dashboard counts
    @Query("SELECT COUNT(lr) FROM LeaveRequest lr WHERE lr.status = :status")
    long countByStatus(@Param("status") LeaveStatus status);

    // Today's exits/entries via GatePass
    @Query("SELECT lr FROM LeaveRequest lr JOIN lr.gatePass gp WHERE gp.exitTime >= :startOfDay AND gp.exitTime < :endOfDay")
    List<LeaveRequest> findTodayExits(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);
}
