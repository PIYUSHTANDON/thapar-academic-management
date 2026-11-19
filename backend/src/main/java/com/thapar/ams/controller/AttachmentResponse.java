package com.thapar.ams.controller;

import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

final class AttachmentResponse {
  private AttachmentResponse() { }

  static ResponseEntity<byte[]> attachment(byte[] data, String filename) {
    return ResponseEntity.ok()
      .header(
        "Content-Disposition",
        ContentDisposition.attachment()
          .filename(filename, StandardCharsets.UTF_8)
          .build()
          .toString()
      )
      .header("X-Content-Type-Options", "nosniff")
      .contentType(MediaType.APPLICATION_OCTET_STREAM)
      .body(data);
  }
}
