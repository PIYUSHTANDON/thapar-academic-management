package com.thapar.ams.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ams_faculty")
public class Faculty {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @OneToOne(optional = false)
  @JoinColumn(unique = true)
  public User user;

  public String department;
}
