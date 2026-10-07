package com.teamforge;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
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
 @GetMapping("/samples")
 Map<?, ?> samples(@RequestParam(defaultValue="0") int offset) {
  if (offset < 0 || offset > 800) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
  try { return ai.get().uri("/profiles/samples?offset={offset}",offset).retrieve().body(Map.class); }
  catch (RestClientException ex) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE); }
 }
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
 record ProjectDraft(@NotBlank @Size(max=80) String name,
  @NotBlank @Size(max=1000) String description, @NotBlank @Size(max=80) String domain,
  @NotBlank @Size(max=80) String stage,
  @NotEmpty @Size(max=20) List<@NotBlank @Size(max=80) String> technologies,
  @NotEmpty @Size(max=4) List<@NotBlank @Size(max=80) String> rolesNeeded,
  @Min(1) @Max(60) int weeklyHours) {}
 record TeamRequest(@NotNull @Valid OnboardingController.ProfileDraft profile, @NotNull @Valid ProjectDraft project) {}
 @PostMapping("/teams")
 Map<?, ?> teams(@Valid @RequestBody TeamRequest request) {
  new OnboardingController().validate(request.profile());
  if (request.project().rolesNeeded().stream().distinct().count() != request.project().rolesNeeded().size()) {
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Roles must be unique");
  }
  try { return ai.post().uri("/teams/demo").body(request).retrieve().body(Map.class); }
  catch (RestClientException ex) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Team recommendations temporarily unavailable"); }
 }
}
