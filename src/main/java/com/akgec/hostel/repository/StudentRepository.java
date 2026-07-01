package com.akgec.hostel.repository;

import com.akgec.hostel.model.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByRollNumber(String rollNumber);
    Optional<Student> findByStudentNo(String studentNo);
    Optional<Student> findByUserId(Long userId);
    boolean existsByRollNumber(String rollNumber);
    boolean existsByStudentNo(String studentNo);

    @Query("SELECT s FROM Student s WHERE s.user.email = :email")
    Optional<Student> findByUserEmail(String email);
}
