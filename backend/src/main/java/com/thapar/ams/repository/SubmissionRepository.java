package com.thapar.ams.repository;

import com.thapar.ams.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
  java.util.List<Submission> findByAssessmentCourseFacultyId(Long id);
  java.util.List<Submission> findByStudentId(Long id);
  java.util.Optional<Submission> findByAssessmentIdAndStudentId(
    Long assessmentId,
    Long studentId
  );
}
