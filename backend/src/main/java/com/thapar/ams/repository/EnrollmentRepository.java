package com.thapar.ams.repository;

import com.thapar.ams.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
  java.util.List<Enrollment> findByCourseId(Long id);
  boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);
  java.util.List<Enrollment> findByStudentIdAndActiveTrue(Long id);
}
