package com.thapar.ams.repository;

import com.thapar.ams.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository
  extends JpaRepository<Notification, Long>
{
  java.util.List<Notification> findByUserId(Long id);
}
