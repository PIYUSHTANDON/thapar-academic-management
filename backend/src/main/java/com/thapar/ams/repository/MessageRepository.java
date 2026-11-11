package com.thapar.ams.repository;

import com.thapar.ams.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {
  java.util.List<Message> findBySenderIdOrReceiverIdOrderBySentAtAsc(
    Long sender,
    Long receiver
  );
}
