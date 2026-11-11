package com.thapar.ams.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ams_notification")
public class Notification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(optional = false)
  public User user;

  @Column(nullable = false)
  public String title;

  public boolean read;
}
