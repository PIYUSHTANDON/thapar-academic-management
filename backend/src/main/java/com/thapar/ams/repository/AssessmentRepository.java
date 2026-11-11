package com.thapar.ams.repository;

import com.thapar.ams.entity.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {
  java.util.List<Assessment> findByCourseFacultyId(Long id);
  java.util.List<Assessment> findByCourseIdIn(java.util.Collection<Long> ids);
}
