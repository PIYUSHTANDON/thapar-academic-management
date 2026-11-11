package com.thapar.ams.entity;

import jakarta.persistence.*;

@Entity
@Table(
  name = "ams_enrollment",
  uniqueConstraints = @UniqueConstraint(
    columnNames = { "student_id", "course_id" }
  )
)
public class Enrollment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(optional = false)
  public Student student;

  @ManyToOne(optional = false)
  public Course course;

  @Column(nullable = false)
  public boolean active = true;
}
