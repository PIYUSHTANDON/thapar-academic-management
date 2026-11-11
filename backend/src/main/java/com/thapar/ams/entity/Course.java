package com.thapar.ams.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ams_course")
public class Course {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(optional = false)
  public Faculty faculty;

  @Column(nullable = false, unique = true)
  public String code;

  @Column(nullable = false)
  public String title;

  public String section;

  @Column(length = 2000)
  public String description;
}
