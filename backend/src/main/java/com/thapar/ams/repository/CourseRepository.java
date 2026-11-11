package com.thapar.ams.repository;

import com.thapar.ams.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {
  java.util.List<Course> findByFacultyId(Long id);
}
