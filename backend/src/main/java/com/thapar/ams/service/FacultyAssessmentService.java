package com.thapar.ams.service;

import com.thapar.ams.contract.AssessmentContracts.FacultyAssessment;
import com.thapar.ams.contract.AssessmentContracts.FacultySubmission;
import com.thapar.ams.contract.AssessmentContracts.IdResponse;
import com.thapar.ams.entity.Assessment;
import com.thapar.ams.entity.Grade;
import com.thapar.ams.entity.Submission;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class FacultyAssessmentService {
  private final FacultyService s;

  public FacultyAssessmentService(FacultyService s) {
    this.s = s;
  }

  public List<FacultyAssessment> assessments(String name) {
    return s.assessments()
      .findByCourseFacultyId(s.faculty(name).id)
      .stream()
      .map(a -> new FacultyAssessment(
        a.id,
        a.course.id,
        a.course.title,
        a.title,
        a.kind,
        a.deadline,
        a.published
      ))
      .toList();
  }

  public IdResponse create(String name, Long courseId, String title, String kind, LocalDateTime deadline) {
    var assessment = new Assessment();
    assessment.course = s.course(name, courseId);
    assessment.title = title;
    assessment.kind = kind;
    assessment.deadline = deadline;
    s.assessments().save(assessment);
    return new IdResponse(assessment.id);
  }

  public List<FacultySubmission> submissions(String name) {
    return s.submissions().findByAssessmentCourseFacultyId(s.faculty(name).id)
      .stream()
      .map(submission -> {
        var grade = s.grades().findAll().stream()
          .filter(g -> g.submission.id.equals(submission.id)).findFirst();
        return new FacultySubmission(
          submission.id,
          submission.assessment.title,
          submission.student.user.name,
          submission.content,
          grade.map(g -> g.marks.toString()).orElse("Ungraded"),
          grade.map(g -> Objects.toString(g.feedback, "")).orElse(""),
          submission.fileName
        );
      }).toList();
  }

  public void grade(String name, Long submissionId, int marks, String feedback) {
    var submission = s.submissions().findById(submissionId).orElseThrow();
    s.course(name, submission.assessment.course.id);
    var grade = s.grades().findAll().stream()
      .filter(g -> g.submission.id.equals(submissionId))
      .findFirst().orElseGet(Grade::new);
    grade.submission = submission;
    grade.marks = marks;
    if (feedback != null) grade.feedback = feedback;
    grade.published = false;
    s.grades().save(grade);
  }

  public void publish(String name, Long assessmentId) {
    var assessment = s.assessments().findById(assessmentId).orElseThrow();
    s.course(name, assessment.course.id);
    assessment.published = true;
    s.assessments().save(assessment);
    s.grades().findAll().stream()
      .filter(g -> g.submission.assessment.id.equals(assessmentId))
      .forEach(g -> { g.published = true; s.grades().save(g); });
  }

  public Submission submissionFile(String name, Long submissionId) {
    var submission = s.submissions().findById(submissionId).orElseThrow();
    s.course(name, submission.assessment.course.id);
    if (submission.fileContent == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    return submission;
  }
}
