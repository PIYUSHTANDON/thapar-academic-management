package com.thapar.ams.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "ams_calendarevent")
public class CalendarEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(optional = false)
  public Faculty faculty;

  @Column(nullable = false)
  public String title;

  @Column(nullable = false)
  public java.time.LocalDate eventDate;
}
