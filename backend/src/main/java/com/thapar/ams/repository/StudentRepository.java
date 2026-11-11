package com.thapar.ams.repository;

import com.thapar.ams.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {
  java.util.Optional<Student> findByUserId(Long id);
}
