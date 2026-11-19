package com.thapar.ams.controller;

import com.thapar.ams.contract.AssessmentContracts.StudentAssessment;
import com.thapar.ams.contract.AssessmentContracts.StudentResults;
import com.thapar.ams.contract.AssessmentContracts.SubmissionResponse;
import com.thapar.ams.service.StudentAssessmentService;
import com.thapar.ams.service.SubmissionService;
import java.security.Principal;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Transactional
@RequestMapping("/api/v1/student")
public class StudentAssessmentController {
  private final StudentAssessmentService assessmentService;
  private final SubmissionService submissions;

  public StudentAssessmentController(StudentAssessmentService assessmentService, SubmissionService submissions) {
    this.assessmentService = assessmentService;
    this.submissions = submissions;
  }

  @GetMapping("/assessments")
  List<StudentAssessment> assessments(Principal p) {
    return assessmentService.assessments(p.getName());
  }

  @GetMapping("/results")
  StudentResults results(Principal p) {
    return assessmentService.resultsSummary(p.getName());
  }

  @GetMapping("/submissions")
  List<StudentAssessment> submissions(Principal p) {
    return assessmentService.submissions(p.getName());
  }

  @PostMapping(
    value = "/submissions",
    consumes = MediaType.MULTIPART_FORM_DATA_VALUE
  )
  SubmissionResponse submit(
    Principal p,
    @RequestParam Long assessmentId,
    @RequestParam(required = false) String content,
    @RequestParam(required = false) MultipartFile file
  ) throws Exception {
    var submission = submissions.submit(p.getName(), assessmentId, content, file);
    return new SubmissionResponse(submission.id, "Assignment submitted");
  }

  @GetMapping("/submissions/{id}/file")
  ResponseEntity<byte[]> submissionFile(Principal p, @PathVariable Long id) {
    var sub = assessmentService.submissionFile(p.getName(), id);
    return AttachmentResponse.attachment(sub.fileContent, sub.fileName);
  }
}
