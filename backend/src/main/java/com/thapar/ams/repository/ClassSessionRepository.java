package com.thapar.ams.repository;

import com.thapar.ams.entity.ClassSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassSessionRepository
  extends JpaRepository<ClassSession, Long>
{
  java.util.List<ClassSession> findByCourseFacultyId(Long id);
  java.util.Optional<ClassSession> findByTokenHash(String hash);
  java.util.List<ClassSession> findByCourseIdIn(java.util.Collection<Long> ids);
}
