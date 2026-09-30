package com.teamforge;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

class DemoRecommendationControllerTest {
 private OnboardingController.ProfileDraft profile() {
  return new OnboardingController.ProfileDraft("Demo", "Backend engineer", List.of("Java"), List.of("Education"), List.of("Frontend engineer"), 8, "Portfolio project", "Structured", "UTC", List.of(20));
 }
 @Test void forwardsProfileAndReturnsAiResponse() throws Exception {
  var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
  var requestBody = new AtomicReference<String>();
  server.createContext("/recommendations/demo", exchange -> {
   requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
   var bytes = "{\"accountType\":\"DEMO\",\"recommendations\":[]}".getBytes(StandardCharsets.UTF_8);
   exchange.getResponseHeaders().set("Content-Type", "application/json");
   exchange.sendResponseHeaders(200, bytes.length);
   exchange.getResponseBody().write(bytes); exchange.close();
  });
  server.start();
  try {
   var controller = new DemoRecommendationController("http://127.0.0.1:" + server.getAddress().getPort());
   assertThat(controller.recommendations(profile()).get("accountType")).isEqualTo("DEMO");
   assertThat(requestBody.get()).contains("Backend engineer", "Education", "rolesSought");
  } finally { server.stop(0); }
 }
 @Test void convertsAiFailuresIntoServiceUnavailable() throws Exception {
  var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
  server.createContext("/recommendations/demo", exchange -> { exchange.sendResponseHeaders(500, -1); exchange.close(); });
  server.start();
  try {
   var controller = new DemoRecommendationController("http://127.0.0.1:" + server.getAddress().getPort());
   assertThatThrownBy(() -> controller.recommendations(profile())).isInstanceOfSatisfying(ResponseStatusException.class, ex -> assertThat(ex.getStatusCode().value()).isEqualTo(503));
  } finally { server.stop(0); }
 }
}
