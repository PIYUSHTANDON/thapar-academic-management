package com.thapar.ams.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ams_learningresource")
public class LearningResource {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(optional = false)
  public Course course;

  @Column(nullable = false)
  public String title;

  public String kind;

  @Lob
  @Column(columnDefinition = "longblob")
  public byte[] content;

  public String contentType;

  @Column(length = 4000)
  public String description;

  public java.time.LocalDateTime publishedAt = java.time.LocalDateTime.now(
    java.time.ZoneId.of("Asia/Kolkata")
  );
}
