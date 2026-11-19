package com.thapar.ams.contract;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class AssessmentContracts {
  private AssessmentContracts() { }

  public record FacultyAssessment(
    long id,
    long courseId,
    String course,
    String title,
    String kind,
    LocalDateTime deadline,
    boolean published
  ) { }

  public record CreateAssessmentRequest(
    @NotNull Long courseId,
    @NotBlank @Size(max = 255) String title,
    @NotBlank @Pattern(regexp = "Assignment|Quiz") String kind,
    @NotNull @Future LocalDateTime deadline
  ) { }

  public record FacultySubmission(
    long id,
    String assessment,
    String student,
    String content,
    String marks,
    String feedback,
    String fileName
  ) { }

  public record GradeRequest(
    @Min(0) @Max(100) int marks,
    @Size(max = 2000) String feedback
  ) { }

  public record StudentAssessment(
    long id,
    String title,
    long courseId,
    String course,
    String kind,
    LocalDateTime deadline,
    String status,
    Long submissionId,
    LocalDateTime submittedAt,
    String fileName,
    String content,
    Integer marks,
    String feedback
  ) { }

  public record StudentResults(
    BigDecimal officialCgpa,
    List<StudentAssessment> results
  ) { }

  public record IdResponse(long id) { }

  public record SubmissionResponse(long id, String message) { }
}
