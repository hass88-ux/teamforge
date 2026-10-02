package com.teamforge;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
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
class DescriptionController {
 private final RestClient ai;
 DescriptionController(@Value("${teamforge.ai-url:http://127.0.0.1:8001}") String url) {
  var factory=new SimpleClientHttpRequestFactory(); factory.setConnectTimeout(Duration.ofSeconds(3)); factory.setReadTimeout(Duration.ofSeconds(5));
  ai=RestClient.builder().baseUrl(url).requestFactory(factory).build();
 }
 record Draft(@NotBlank @Size(max=1000) String description) {}
 @PostMapping("/api/onboarding/skills") Map<?,?> suggest(@Valid @RequestBody Draft draft) {
  try { return ai.post().uri("/description/skills").body(draft).retrieve().body(Map.class); }
  catch (RestClientException e) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE); }
 }
}
