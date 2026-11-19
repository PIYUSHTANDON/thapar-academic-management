package com.thapar.ams.desktop.api;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.thapar.ams.contract.AssessmentContracts.CreateAssessmentRequest;
import com.thapar.ams.contract.AuthContracts.UserRole;
import com.thapar.ams.contract.OverviewContracts.CreateEventRequest;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ApiClientTest {
  private HttpServer server;
  private ApiClient client;
  private final AtomicInteger csrfCounter = new AtomicInteger();
  private final List<String> seenCsrfHeaders = new CopyOnWriteArrayList<>();
  private final List<String> seenCookies = new CopyOnWriteArrayList<>();
  private volatile String lastBody;
  private volatile String lastContentType;

  @BeforeEach void start() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/api/v1/auth/csrf", exchange -> {
      String token = "csrf-" + csrfCounter.incrementAndGet();
      exchange.getResponseHeaders().add("Set-Cookie", "JSESSIONID=sess-" + csrfCounter.get() + "; Path=/");
      reply(exchange, 200, "{\"token\":\"" + token + "\",\"headerName\":\"X-CSRF-TOKEN\"}");
    });
    server.createContext("/api/v1/auth/login", exchange -> {
      record(exchange);
      exchange.getResponseHeaders().add("Set-Cookie", "JSESSIONID=rotated; Path=/");
      reply(exchange, 200,
        "{\"id\":7,\"name\":\"Dr. Ada\",\"identifier\":\"FAC001\",\"role\":\"FACULTY\",\"notificationsEnabled\":true}");
    });
    server.createContext("/api/v1/auth/logout", exchange -> {
      record(exchange);
      reply(exchange, 204, "");
    });
    server.createContext("/api/v1/auth/session", exchange -> {
      record(exchange);
      reply(exchange, 200, "{\"authenticated\":false}");
    });
    server.createContext("/api/v1/echo", exchange -> {
      record(exchange);
      reply(exchange, 200, lastBody == null || lastBody.isBlank() ? "{}" : lastBody);
    });
    server.createContext("/api/v1/upload", exchange -> {
      record(exchange);
      reply(exchange, 200, "{\"id\":42,\"message\":\"Assignment submitted\"}");
    });
    server.createContext("/api/v1/file", exchange -> {
      record(exchange);
      byte[] bytes = "PDF-BYTES-123".getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
      exchange.sendResponseHeaders(200, bytes.length);
      exchange.getResponseBody().write(bytes);
      exchange.close();
    });
    server.createContext("/api/v1/error/", exchange -> {
      String code = exchange.getRequestURI().getPath().substring("/api/v1/error/".length());
      int status = Integer.parseInt(code);
      if (status == 400)
        reply(exchange, 400, "{\"message\":\"Please check the supplied fields\",\"errors\":[\"marks: must be <= 100\",\"title: must not be blank\"]}");
      else if (status == 409)
        reply(exchange, 409, "{\"message\":\"Attendance already recorded\"}");
      else
        reply(exchange, status, "{\"message\":\"HTTP " + status + " failure\"}");
    });
    server.start();
    client = new ApiClient(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api/v1"));
  }

  @AfterEach void stop() {
    if (server != null) server.stop(0);
  }

  @Test void managesCookiesAndRotatesCsrfAcrossLoginAndLogout() {
    assertNull(client.session());

    var user = client.login("FAC001", "secret-password-12", UserRole.FACULTY, true);
    assertEquals(7L, user.id());
    assertEquals(UserRole.FACULTY, user.role());
    assertEquals(2, csrfCounter.get(), "Should fetch initial CSRF before login and rotate CSRF after login");
    assertEquals("csrf-1", seenCsrfHeaders.getFirst());

    client.send("POST", "echo", Map.of("ping", "ok"));
    assertEquals("csrf-2", seenCsrfHeaders.get(1));
    assertTrue(seenCookies.getLast().contains("JSESSIONID="));

    client.logout();
    assertEquals("csrf-2", seenCsrfHeaders.get(2));

    client.send("POST", "echo", Map.of("afterLogout", true));
    assertEquals(3, csrfCounter.get(), "Logout must clear CSRF so the next mutation fetches a fresh token");
    assertEquals("csrf-3", seenCsrfHeaders.get(3));
  }

  @Test void serializesJavaTimeAsIsoStringsNotArrays() {
    client.send("POST", "echo", new CreateAssessmentRequest(1L, "Lab 1", "Assignment",
      LocalDateTime.of(2027, 5, 10, 14, 30)));
    assertTrue(lastBody.contains("\"deadline\":\"2027-05-10T14:30:00\""), "Unexpected JSON: " + lastBody);

    client.send("POST", "echo", new CreateEventRequest("Convocation", LocalDate.of(2027, 6, 15)));
    assertTrue(lastBody.contains("\"eventDate\":\"2027-06-15\""), "Unexpected JSON: " + lastBody);
  }

  @Test void parsesStructuredValidationErrorsAndHttpStatuses() {
    var badRequest = assertThrows(ApiClient.ApiException.class, () -> client.get("error/400"));
    assertEquals(400, badRequest.status());
    assertTrue(badRequest.getMessage().contains("Please check the supplied fields"));
    assertTrue(badRequest.getMessage().contains("marks: must be <= 100"));
    assertTrue(badRequest.getMessage().contains("title: must not be blank"));

    for (int status : List.of(401, 403, 409, 429)) {
      var error = assertThrows(ApiClient.ApiException.class, () -> client.get("error/" + status));
      assertEquals(status, error.status());
    }
  }

  @Test void sendsMultipartUploadsAndDownloadsExactBytes(@TempDir Path temp) throws Exception {
    Path txt = temp.resolve("notes.txt");
    Files.writeString(txt, "Hello Thapar");
    var response = client.postMultipart("upload", Map.of("assessmentId", "9", "content", "My work"), txt);
    assertEquals(42L, response.path("id").asLong());
    assertTrue(lastContentType.startsWith("multipart/form-data; boundary=ams-"));
    assertTrue(lastBody.contains("Content-Type: text/plain"));
    assertTrue(lastBody.contains("filename=\"notes.txt\""));
    assertTrue(lastBody.contains("Hello Thapar"));

    Path pdf = temp.resolve("report.pdf");
    Files.write(pdf, new byte[] {'%', 'P', 'D', 'F', '-', '1', '.', '4'});
    client.postMultipart("upload", Map.of("assessmentId", "9"), pdf);
    assertTrue(lastBody.contains("Content-Type: application/pdf"));

    assertArrayEquals("PDF-BYTES-123".getBytes(StandardCharsets.UTF_8), client.download("file"));
  }

  @Test void mapsUnreachableServerToZeroStatus() throws Exception {
    int unusedPort;
    try (var socket = new ServerSocket(0)) { unusedPort = socket.getLocalPort(); }
    var offline = new ApiClient(URI.create("http://127.0.0.1:" + unusedPort + "/api/v1"));
    var error = assertThrows(ApiClient.ApiException.class, offline::session);
    assertEquals(0, error.status());
    assertTrue(error.getMessage().contains(Integer.toString(unusedPort)));
  }

  @Test void resolvesApiBasePrecedencePropertyThenEnvThenDefault() {
    assertEquals(URI.create("http://127.0.0.1:9001/api/v1"),
      ApiClient.resolveBaseUri("http://127.0.0.1:9001/api/v1", "http://127.0.0.1:9002/api/v1"));
    assertEquals(URI.create("http://127.0.0.1:9002/api/v1"),
      ApiClient.resolveBaseUri("   ", "http://127.0.0.1:9002/api/v1"));
    assertEquals(URI.create("http://127.0.0.1:8080/api/v1"),
      ApiClient.resolveBaseUri(null, ""));
  }

  private void record(HttpExchange exchange) throws IOException {
    String csrf = exchange.getRequestHeaders().getFirst("X-CSRF-TOKEN");
    if (csrf != null) seenCsrfHeaders.add(csrf);
    String cookie = exchange.getRequestHeaders().getFirst("Cookie");
    if (cookie != null) seenCookies.add(cookie);
    lastContentType = exchange.getRequestHeaders().getFirst("Content-Type");
    lastBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
  }

  private static void reply(HttpExchange exchange, int status, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
    if (bytes.length > 0) exchange.getResponseBody().write(bytes);
    exchange.close();
  }
}
