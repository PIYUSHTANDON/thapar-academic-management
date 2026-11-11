package com.thapar.ams.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ams_user")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false, unique = true, length = 120)
  public String identifier;

  @Column(nullable = false)
  public String passwordHash;

  @Column(nullable = false)
  public String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public Role role;

  public boolean notificationsEnabled;

  @Column(nullable = false)
  public boolean localOnly;
}
