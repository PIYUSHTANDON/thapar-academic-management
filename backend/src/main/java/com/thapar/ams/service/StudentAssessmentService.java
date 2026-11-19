package com.thapar.ams.service;

import com.thapar.ams.contract.AssessmentContracts.StudentAssessment;
import com.thapar.ams.contract.AssessmentContracts.StudentResults;
import com.thapar.ams.entity.Assessment;
import com.thapar.ams.entity.Submission;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class StudentAssessmentService {
  private final FacultyService s;
  private final StudentService access;

  public StudentAssessmentService(FacultyService s, StudentService access) {
    this.s = s;
    this.access = access;
  }

  public List<Assessment> available(String name) {
    return s.assessments().findByCourseIdIn(access.courseIds(name));
  }

  public StudentAssessment assessment(String name, Assessment a) {
    var st = access.student(name);
    var submission = s.submissions().findByAssessmentIdAndStudentId(a.id, st.id).orElse(null);
    var grade = submission == null ? null : s.grades().findBySubmissionId(submission.id).orElse(null);
    boolean released = grade != null && grade.published && a.published;
    String status = submission != null
      ? released ? "Graded" : "Submitted"
      : StudentService.now().isAfter(a.deadline)
        ? "Overdue"
        : StudentService.now().plusDays(2).isAfter(a.deadline)
          ? "Due Soon"
          : "Pending";
    return new StudentAssessment(
      a.id,
      a.title,
      a.course.id,
      a.course.title,
      a.kind,
      a.deadline,
      status,
      submission == null ? null : submission.id,
      submission == null ? null : submission.submittedAt,
      submission == null ? null : submission.fileName,
      submission == null ? null : submission.content,
      released ? grade.marks : null,
      released ? grade.feedback : null
    );
  }

  public List<StudentAssessment> assessments(String name) {
    return available(name)
      .stream()
      .sorted(Comparator.comparing(a -> a.deadline))
      .map(a -> assessment(name, a))
      .toList();
  }

  public List<StudentAssessment> results(String name) {
    return assessments(name).stream().filter(a -> a.marks() != null).toList();
  }

  public StudentResults resultsSummary(String name) {
    return new StudentResults(access.student(name).officialCgpa, results(name));
  }

  public List<StudentAssessment> submissions(String name) {
    return assessments(name).stream().filter(a -> a.submissionId() != null).toList();
  }

  public Submission submissionFile(String name, Long id) {
    var submission = s.submissions().findById(id).orElseThrow();
    if (!submission.student.id.equals(access.student(name).id))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    access.course(name, submission.assessment.course.id);
    if (submission.fileContent == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    return submission;
  }
}
