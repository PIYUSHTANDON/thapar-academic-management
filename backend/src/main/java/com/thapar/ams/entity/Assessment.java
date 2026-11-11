package com.thapar.ams.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ams_assessment")
public class Assessment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(optional = false)
  public Course course;

  @Column(nullable = false)
  public String title;

  public String kind;

  @Column(nullable = false)
  public java.time.LocalDateTime deadline;

  public boolean published;
}
