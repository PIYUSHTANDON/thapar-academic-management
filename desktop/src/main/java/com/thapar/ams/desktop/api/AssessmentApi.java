package com.thapar.ams.desktop.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.thapar.ams.contract.AssessmentContracts.CreateAssessmentRequest;
import com.thapar.ams.contract.AssessmentContracts.FacultyAssessment;
import com.thapar.ams.contract.AssessmentContracts.FacultySubmission;
import com.thapar.ams.contract.AssessmentContracts.GradeRequest;
import com.thapar.ams.contract.AssessmentContracts.IdResponse;
import com.thapar.ams.contract.AssessmentContracts.StudentAssessment;
import com.thapar.ams.contract.AssessmentContracts.StudentResults;
import com.thapar.ams.contract.CourseContracts.FacultyCourse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AssessmentApi {
  private final ApiClient api;

  public AssessmentApi(ApiClient api) { this.api = api; }

  public List<FacultyCourse> facultyCourses() {
    return api.get("faculty/courses", new TypeReference<>() { });
  }

  public List<FacultyAssessment> facultyAssessments() {
    return api.get("faculty/assessments", new TypeReference<>() { });
  }

  public List<FacultySubmission> facultySubmissions() {
    return api.get("faculty/submissions", new TypeReference<>() { });
  }

  public IdResponse create(long courseId, String title, String kind, LocalDateTime deadline) {
    return api.post("faculty/assessments", new CreateAssessmentRequest(courseId, title, kind, deadline), IdResponse.class);
  }

  public void grade(long submissionId, int marks, String feedback) {
    api.send("POST", "faculty/submissions/" + submissionId + "/grade", new GradeRequest(marks, feedback));
  }

  public void publish(long assessmentId) {
    api.send("POST", "faculty/assessments/" + assessmentId + "/publish", null);
  }

  public byte[] facultyFile(long submissionId) {
    return api.download("faculty/submissions/" + submissionId + "/file");
  }

  public List<StudentAssessment> studentAssessments() {
    return api.get("student/assessments", new TypeReference<>() { });
  }

  public StudentResults results() { return api.get("student/results", StudentResults.class); }

  public void submit(long assessmentId, String content, Path file) throws IOException {
    if (content.length() > 4000) throw new IllegalArgumentException("Submission text must be at most 4000 characters.");
    if (file == null && content.isBlank())
      throw new IllegalArgumentException("Enter submission text or choose a PDF/TXT file.");
    if (file != null) {
      String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
      if (!name.endsWith(".pdf") && !name.endsWith(".txt"))
        throw new IllegalArgumentException("Choose a PDF or UTF-8 TXT file.");
      if (Files.size(file) > 5 * 1024 * 1024)
        throw new IllegalArgumentException("Submission files must be at most 5 MB.");
    }
    api.postMultipart("student/submissions", Map.of("assessmentId", Long.toString(assessmentId),
      "content", content), file);
  }

  public byte[] studentFile(long submissionId) {
    return api.download("student/submissions/" + submissionId + "/file");
  }
}
