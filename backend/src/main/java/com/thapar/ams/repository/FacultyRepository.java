package com.thapar.ams.repository;

import com.thapar.ams.entity.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacultyRepository extends JpaRepository<Faculty, Long> {
  java.util.Optional<Faculty> findByUserId(Long id);
}
