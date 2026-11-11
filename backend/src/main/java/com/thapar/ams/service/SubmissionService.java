package com.thapar.ams.service;

import com.thapar.ams.entity.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class SubmissionService {

  private final FacultyService s;
  private final StudentService students;

  public SubmissionService(FacultyService s, StudentService students) {
    this.s = s;
    this.students = students;
  }

  public Submission submit(
    String name,
    Long assessmentId,
    String content,
    MultipartFile file
  ) throws java.io.IOException {
    var student = students.student(name);
    var a = s.assessments().findById(assessmentId).orElseThrow();
    students.course(name, a.course.id);
    if (
      a.deadline.isBefore(StudentService.now())
    ) throw new ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "The submission deadline has passed"
    );
    if (
      s
        .submissions()
        .findByAssessmentIdAndStudentId(a.id, student.id)
        .isPresent()
    ) throw new ResponseStatusException(
      HttpStatus.CONFLICT,
      "This assessment has already been submitted"
    );
    if (
      content != null && content.length() > 4000
    ) throw new ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "Submission text must not exceed 4000 characters"
    );
    if (
      (content == null || content.isBlank()) && (file == null || file.isEmpty())
    ) throw new ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "Provide submission text or a supported file"
    );
    var submission = new Submission();
    submission.student = student;
    submission.assessment = a;
    submission.content = Objects.toString(content, "");
    submission.submittedAt = StudentService.now();
    if (file != null && !file.isEmpty()) {
      if (file.getSize() > 5 * 1024 * 1024) throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Submission files must be at most 5 MB"
      );
      String original = Objects.toString(file.getOriginalFilename(), "");
      String filename = original.replace('\\', '/');
      filename = filename.substring(filename.lastIndexOf('/') + 1);
      if (
        filename.isBlank() ||
        filename.length() > 200 ||
        filename.chars().anyMatch(c -> c < 32 || c == 127)
      ) throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Invalid file name"
      );
      byte[] bytes = file.getBytes();
      String extension = filename.toLowerCase(Locale.ROOT);
      if (extension.endsWith(".pdf")) {
        if (
          bytes.length < 5 ||
          !new String(bytes, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-")
        ) throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "The PDF file signature is invalid"
        );
        submission.contentType = "application/pdf";
      } else if (extension.endsWith(".txt")) {
        try {
          String text = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString();
          if (text.indexOf(0) >= 0) throw new CharacterCodingException();
        } catch (CharacterCodingException e) {
          throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Text files must contain valid UTF-8 text"
          );
        }
        submission.contentType = "text/plain";
      } else throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Supported files: PDF and UTF-8 TXT only"
      );
      submission.fileName = filename;
      submission.fileContent = bytes;
    }
    s.submissions().save(submission);
    var note = new Notification();
    note.user = a.course.faculty.user;
    note.title = student.user.name + " submitted " + a.title;
    s.notifications().save(note);
    return submission;
  }
}
