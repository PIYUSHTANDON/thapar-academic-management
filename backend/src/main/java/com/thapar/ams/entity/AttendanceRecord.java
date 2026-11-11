package com.thapar.ams.entity;

import jakarta.persistence.*;

@Entity
@Table(
  name = "ams_attendancerecord",
  uniqueConstraints = @UniqueConstraint(
    columnNames = { "student_id", "class_session_id" }
  )
)
public class AttendanceRecord {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(optional = false)
  public Student student;

  @ManyToOne(optional = false)
  public ClassSession classSession;

  public java.time.LocalDateTime recordedAt;
}
