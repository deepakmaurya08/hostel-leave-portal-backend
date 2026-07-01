package com.akgec.hostel.repository;

import com.akgec.hostel.model.entity.GatePass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GatePassRepository extends JpaRepository<GatePass, Long> {
    Optional<GatePass> findByQrToken(String qrToken);
    Optional<GatePass> findByLeaveRequestId(Long leaveId);
    Optional<GatePass> findByLeaveRequestLeavePassNumber(String leavePassNumber);

    @Query("SELECT gp FROM GatePass gp WHERE gp.exitTime IS NOT NULL AND gp.entryTime IS NULL")
    List<GatePass> findStudentsCurrentlyOnLeave();

    @Query("SELECT gp FROM GatePass gp WHERE gp.exitTime >= :startOfDay AND gp.exitTime < :endOfDay")
    List<GatePass> findTodayExits(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT gp FROM GatePass gp WHERE gp.entryTime >= :startOfDay AND gp.entryTime < :endOfDay")
    List<GatePass> findTodayEntries(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);
}
