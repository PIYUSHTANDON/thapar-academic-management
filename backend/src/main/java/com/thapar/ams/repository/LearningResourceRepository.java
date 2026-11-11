package com.thapar.ams.repository;

import com.thapar.ams.entity.LearningResource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningResourceRepository
  extends JpaRepository<LearningResource, Long>
{
  java.util.List<LearningResource> findByCourseId(Long id);
  java.util.List<LearningResource> findByCourseIdIn(
    java.util.Collection<Long> ids
  );
}
