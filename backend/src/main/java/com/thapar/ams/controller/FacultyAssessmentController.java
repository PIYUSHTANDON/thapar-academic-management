package com.thapar.ams.controller;

import com.thapar.ams.contract.AssessmentContracts.CreateAssessmentRequest;
import com.thapar.ams.contract.AssessmentContracts.FacultyAssessment;
import com.thapar.ams.contract.AssessmentContracts.FacultySubmission;
import com.thapar.ams.contract.AssessmentContracts.GradeRequest;
import com.thapar.ams.contract.AssessmentContracts.IdResponse;
import com.thapar.ams.service.FacultyAssessmentService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Transactional
@RequestMapping("/api/v1/faculty")
public class FacultyAssessmentController {
  private final FacultyAssessmentService assessmentService;

  public FacultyAssessmentController(FacultyAssessmentService assessmentService) {
    this.assessmentService = assessmentService;
  }

  @GetMapping("/assessments")
  List<FacultyAssessment> assessments(Principal p) {
    return assessmentService.assessments(p.getName());
  }

  @PostMapping("/assessments")
  IdResponse createAssessment(Principal p, @Valid @RequestBody CreateAssessmentRequest b) {
    return assessmentService.create(p.getName(), b.courseId(), b.title(), b.kind(), b.deadline());
  }

  @GetMapping("/submissions")
  List<FacultySubmission> submissions(Principal p) {
    return assessmentService.submissions(p.getName());
  }

  @PostMapping("/submissions/{id}/grade")
  void grade(Principal p, @PathVariable Long id, @Valid @RequestBody GradeRequest b) {
    assessmentService.grade(p.getName(), id, b.marks(), b.feedback());
  }

  @PostMapping("/assessments/{id}/publish")
  void publish(Principal p, @PathVariable Long id) {
    assessmentService.publish(p.getName(), id);
  }

  @GetMapping("/submissions/{id}/file")
  ResponseEntity<byte[]> submissionFile(Principal p, @PathVariable Long id) {
    var sub = assessmentService.submissionFile(p.getName(), id);
    return AttachmentResponse.attachment(sub.fileContent, sub.fileName);
  }
}
