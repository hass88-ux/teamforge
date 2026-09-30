package com.teamforge;

import jakarta.validation.Valid;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/demo")
class DemoRecommendationController {
 private final RestClient ai;
 DemoRecommendationController(@Value("${teamforge.ai-url:http://127.0.0.1:8001}") String aiUrl) {
  var factory = new SimpleClientHttpRequestFactory();
  factory.setConnectTimeout(Duration.ofSeconds(3));
  factory.setReadTimeout(Duration.ofSeconds(10));
  ai = RestClient.builder().baseUrl(aiUrl).requestFactory(factory).build();
 }
 @PostMapping("/recommendations")
 Map<?, ?> recommendations(@Valid @RequestBody OnboardingController.ProfileDraft profile) {
  new OnboardingController().validate(profile);
  try { return ai.post().uri("/recommendations/demo").body(profile).retrieve().body(Map.class); }
  catch (RestClientException ex) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Recommendations temporarily unavailable"); }
 }
}
