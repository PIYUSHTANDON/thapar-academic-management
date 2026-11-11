package com.thapar.ams.repository;

import com.thapar.ams.entity.Grade;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GradeRepository extends JpaRepository<Grade, Long> {
  java.util.Optional<Grade> findBySubmissionId(Long id);
}
