package com.thapar.ams.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ams_message")
public class Message {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(optional = false)
  public User sender;

  @ManyToOne(optional = false)
  public User receiver;

  @Column(nullable = false, length = 4000)
  public String body;

  @Column(nullable = false)
  public java.time.LocalDateTime sentAt;
}
