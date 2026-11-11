package com.thapar.ams.repository;

import com.thapar.ams.entity.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalendarEventRepository
  extends JpaRepository<CalendarEvent, Long>
{
  java.util.List<CalendarEvent> findByFacultyId(Long id);
  java.util.List<CalendarEvent> findByFacultyIdIn(
    java.util.Collection<Long> ids
  );
}
