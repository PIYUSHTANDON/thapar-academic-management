package com.thapar.ams.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ams_classsession")
public class ClassSession {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(optional = false)
  public Course course;

  @Column(nullable = false)
  public java.time.LocalDateTime startsAt;

  @Column(nullable = false)
  public java.time.LocalDateTime endsAt;

  public String room;
  public boolean cancelled;
  public String tokenHash;
  public java.time.LocalDateTime tokenExpiresAt;
}
