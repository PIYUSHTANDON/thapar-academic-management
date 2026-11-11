package com.thapar.ams.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ams_student")
public class Student {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @OneToOne(optional = false)
  @JoinColumn(unique = true)
  public User user;

  @Column(unique = true)
  public String enrollmentNumber;

  public String program;
  public Integer academicYear;
  public Integer semester;

  @Column(precision = 4, scale = 2)
  public java.math.BigDecimal officialCgpa;
}
