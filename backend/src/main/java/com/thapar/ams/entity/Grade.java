package com.thapar.ams.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ams_grade")
public class Grade {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @OneToOne(optional = false)
  @JoinColumn(unique = true)
  public Submission submission;

  @Column(nullable = false)
  public Integer marks;

  public boolean published;

  @Column(length = 2000)
  public String feedback;
}
