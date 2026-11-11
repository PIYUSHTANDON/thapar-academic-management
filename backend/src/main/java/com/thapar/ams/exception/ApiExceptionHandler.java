package com.thapar.ams.exception;

import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(java.util.NoSuchElementException.class)
  public ResponseEntity<?> notFound() {
    return ResponseEntity.status(404).body(
      Map.of("message", "Resource not found")
    );
  }

  @ExceptionHandler(
    org.springframework.dao.DataIntegrityViolationException.class
  )
  public ResponseEntity<?> conflict() {
    return ResponseEntity.status(409).body(
      Map.of("message", "This operation conflicts with an existing record")
    );
  }

  @ExceptionHandler(
    org.springframework.web.server.ResponseStatusException.class
  )
  public ResponseEntity<?> requestError(
    org.springframework.web.server.ResponseStatusException e
  ) {
    return ResponseEntity.status(e.getStatusCode()).body(
      Map.of("message", Objects.toString(e.getReason(), "Request failed"))
    );
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<?> invalid(MethodArgumentNotValidException e) {
    return ResponseEntity.badRequest().body(
      Map.of(
        "message",
        "Please check the supplied fields",
        "errors",
        e
          .getBindingResult()
          .getFieldErrors()
          .stream()
          .map(x -> x.getField() + ": " + x.getDefaultMessage())
          .toList()
      )
    );
  }
}
