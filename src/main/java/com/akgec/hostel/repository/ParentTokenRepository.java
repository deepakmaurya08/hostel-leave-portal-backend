package com.akgec.hostel.repository;

import com.akgec.hostel.model.entity.ParentToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ParentTokenRepository extends JpaRepository<ParentToken, Long> {
    Optional<ParentToken> findByToken(String token);

    @Query("SELECT pt FROM ParentToken pt WHERE pt.leaveRequest.id = :leaveId AND pt.used = false " +
           "AND pt.expiryTime > :now ORDER BY pt.createdAt DESC")
    Optional<ParentToken> findValidTokenByLeaveId(@Param("leaveId") Long leaveId,
                                                   @Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("DELETE FROM ParentToken pt WHERE pt.expiryTime < :now AND pt.used = false")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
}
