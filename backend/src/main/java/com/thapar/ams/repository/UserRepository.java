package com.thapar.ams.repository;

import com.thapar.ams.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
  java.util.Optional<User> findByIdentifier(String identifier);
}
