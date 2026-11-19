package com.thapar.ams.exception;

import com.thapar.ams.contract.AuthContracts.ApiErrorResponse;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(NoSuchElementException.class)
  public ResponseEntity<ApiErrorResponse> notFound() {
    return ResponseEntity.status(404).body(
      new ApiErrorResponse("Resource not found")
    );
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiErrorResponse> conflict() {
    return ResponseEntity.status(409).body(
      new ApiErrorResponse("This operation conflicts with an existing record")
    );
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ApiErrorResponse> requestError(
    ResponseStatusException e
  ) {
    return ResponseEntity.status(e.getStatusCode()).body(
      new ApiErrorResponse(Objects.toString(e.getReason(), "Request failed"))
    );
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> invalid(
    MethodArgumentNotValidException e
  ) {
    return ResponseEntity.badRequest().body(
      new ApiErrorResponse(
        "Please check the supplied fields",
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
