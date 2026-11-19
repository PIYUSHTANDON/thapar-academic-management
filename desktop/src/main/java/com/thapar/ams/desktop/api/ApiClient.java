package com.thapar.ams.desktop.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.thapar.ams.contract.AuthContracts.ApiErrorResponse;
import com.thapar.ams.contract.AuthContracts.CsrfTokenResponse;
import com.thapar.ams.contract.AuthContracts.LoginRequest;
import com.thapar.ams.contract.AuthContracts.SessionResponse;
import com.thapar.ams.contract.AuthContracts.UserResponse;
import com.thapar.ams.contract.AuthContracts.UserRole;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class ApiClient {
  public static final class ApiException extends RuntimeException {
    private final int status;
    public ApiException(int status, String message) {
      super(message);
      this.status = status;
    }
    public int status() { return status; }
  }

  private final URI base;
  private final HttpClient http;
  private final ObjectMapper json = newObjectMapper();
  private String csrfToken;
  private String csrfHeader;

  public ApiClient() {
    this(resolveBaseUri(System.getProperty("ams.api.base"), System.getenv("AMS_API_BASE")));
  }

  static URI resolveBaseUri(String propertyValue, String envValue) {
    if (propertyValue != null && !propertyValue.isBlank()) return URI.create(propertyValue.trim());
    if (envValue != null && !envValue.isBlank()) return URI.create(envValue.trim());
    return URI.create("http://127.0.0.1:8080/api/v1");
  }

  public ApiClient(URI baseUri) {
    base = URI.create(baseUri.toString().replaceAll("/+$", "") + "/");
    var cookies = new CookieManager();
    cookies.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
    http = HttpClient.newBuilder().cookieHandler(cookies).connectTimeout(Duration.ofSeconds(5)).build();
  }

  public static ObjectMapper newObjectMapper() {
    return new ObjectMapper()
      .registerModule(new JavaTimeModule())
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  public UserResponse session() {
    SessionResponse result = get("auth/session", SessionResponse.class);
    return result.authenticated() ? result.user() : null;
  }

  public UserResponse login(String identifier, String password, UserRole role, boolean rememberMe) {
    UserResponse user = post("auth/login", new LoginRequest(identifier, password, role, rememberMe), UserResponse.class);
    clearCsrf();
    refreshCsrf();
    return user;
  }

  public void logout() {
    try {
      send("POST", "auth/logout", null);
    } finally {
      clearCsrf();
    }
  }

  public void resetCsrf() { clearCsrf(); }

  public JsonNode get(String path) { return send("GET", path, null); }

  public <T> T get(String path, Class<T> type) {
    return json.convertValue(get(path), type);
  }

  public <T> T get(String path, TypeReference<T> type) {
    return json.convertValue(get(path), type);
  }

  public <T> T post(String path, Object body, Class<T> type) {
    return json.convertValue(send("POST", path, body), type);
  }

  public JsonNode send(String method, String path, Object body) {
    try {
      byte[] payload = body == null ? null : json.writeValueAsBytes(body);
      return parse(execute(method, path, payload, body == null ? null : "application/json"));
    } catch (IOException e) {
      throw new IllegalStateException("Could not read the Java server response.", e);
    }
  }

  public JsonNode postMultipart(String path, Map<String, String> fields, Path file) throws IOException {
    String boundary = "ams-" + UUID.randomUUID();
    var body = new ByteArrayOutputStream();
    for (var field : fields.entrySet()) {
      write(body, "--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + field.getKey() +
        "\"\r\n\r\n" + field.getValue() + "\r\n");
    }
    if (file != null) {
      String name = file.getFileName().toString();
      if (name.contains("\"") || name.contains("\r") || name.contains("\n"))
        throw new IllegalArgumentException("Choose a file with a simple name.");
      String lower = name.toLowerCase(Locale.ROOT);
      String type = lower.endsWith(".pdf") ? "application/pdf" :
        lower.endsWith(".txt") ? "text/plain" : "application/octet-stream";
      write(body, "--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" +
        name + "\"\r\nContent-Type: " + type + "\r\n\r\n");
      body.write(Files.readAllBytes(file));
      write(body, "\r\n");
    }
    write(body, "--" + boundary + "--\r\n");
    return parse(execute("POST", path, body.toByteArray(), "multipart/form-data; boundary=" + boundary));
  }

  public byte[] download(String path) { return execute("GET", path, null, null); }

  private byte[] execute(String method, String path, byte[] body, String contentType) {
    try {
      if (!method.equals("GET") && csrfToken == null) refreshCsrf();
      var request = HttpRequest.newBuilder(base.resolve(path))
        .timeout(Duration.ofSeconds(30)).header("Accept", "application/json");
      if (!method.equals("GET")) request.header(csrfHeader, csrfToken);
      if (contentType != null) request.header("Content-Type", contentType);
      request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() :
        HttpRequest.BodyPublishers.ofByteArray(body));
      var response = http.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
      if (response.statusCode() >= 400) {
        String message = "Request failed (HTTP " + response.statusCode() + ")";
        try {
          ApiErrorResponse error = json.readValue(response.body(), ApiErrorResponse.class);
          if (error.message() != null && !error.message().isBlank()) message = error.message();
          if (error.errors() != null && !error.errors().isEmpty())
            message = message + ": " + String.join("; ", error.errors());
        } catch (Exception ignored) { }
        throw new ApiException(response.statusCode(), message);
      }
      return response.body();
    } catch (IOException e) {
      throw new ApiException(0, "Cannot reach the Java server at " + base.getHost() + ":" + base.getPort());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(0, "The request was interrupted.");
    }
  }

  private JsonNode parse(byte[] data) throws IOException {
    return data.length == 0 ? json.createObjectNode() : json.readTree(data);
  }

  private static void write(ByteArrayOutputStream body, String text) {
    body.writeBytes(text.getBytes(StandardCharsets.UTF_8));
  }

  private void refreshCsrf() {
    CsrfTokenResponse token = get("auth/csrf", CsrfTokenResponse.class);
    csrfToken = token.token();
    csrfHeader = token.headerName();
  }

  private void clearCsrf() {
    csrfToken = null;
    csrfHeader = null;
  }
}
