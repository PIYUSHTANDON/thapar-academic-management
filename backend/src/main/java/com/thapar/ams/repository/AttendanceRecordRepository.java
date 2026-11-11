package com.thapar.ams.repository;

import com.thapar.ams.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRecordRepository
  extends JpaRepository<AttendanceRecord, Long>
{
  java.util.List<AttendanceRecord> findByClassSessionCourseFacultyId(Long id);
  boolean existsByStudentIdAndClassSessionId(Long studentId, Long sessionId);
  java.util.List<AttendanceRecord> findByStudentId(Long id);
}
