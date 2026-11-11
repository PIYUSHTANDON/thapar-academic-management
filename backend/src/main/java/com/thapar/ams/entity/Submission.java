package com.thapar.ams.entity;

import jakarta.persistence.*;

@Entity
@Table(
  name = "ams_submission",
  uniqueConstraints = @UniqueConstraint(
    columnNames = { "student_id", "assessment_id" }
  )
)
public class Submission {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(optional = false)
  public Assessment assessment;

  @ManyToOne(optional = false)
  public Student student;

  @Column(length = 4000)
  public String content;

  public java.time.LocalDateTime submittedAt;

  @Lob
  @Column(columnDefinition = "longblob")
  public byte[] fileContent;

  public String fileName;
  public String contentType;
}
