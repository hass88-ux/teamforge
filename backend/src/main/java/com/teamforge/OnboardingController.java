package com.teamforge;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/onboarding")
class OnboardingController {
 record ProfileDraft(
  @NotBlank @Size(max=60) String displayName,
  @NotBlank @Size(max=80) String role,
  @NotEmpty @Size(max=20) List<@NotBlank @Size(max=60) String> skills,
  @NotEmpty @Size(max=20) List<@NotBlank @Size(max=60) String> interests,
  @NotEmpty @Size(max=10) List<@NotBlank @Size(max=80) String> rolesSought,
  @Min(1) @Max(60) int weeklyHours,
  @NotBlank @Size(max=80) String goal,
  @NotBlank @Size(max=60) String workingStyle,
  @NotBlank @Size(max=80) String timezone,
  @NotEmpty @Size(max=168) List<@Min(0) @Max(167) Integer> availability,
  @Pattern(regexp="INDIVIDUAL|ORGANIZATION") String entityType,
  @Pattern(regexp="PROVIDER|SEEKER|COLLABORATOR") String matchingIntent,
  @Size(max=1000) String description,
  @Size(max=20) List<@NotBlank @Size(max=60) String> neededSkills
 ) {
  ProfileDraft {
   entityType = entityType == null ? "INDIVIDUAL" : entityType;
   matchingIntent = matchingIntent == null ? "COLLABORATOR" : matchingIntent;
   description = description == null ? "" : description;
   neededSkills = neededSkills == null ? List.of() : neededSkills;
  }
  ProfileDraft(String name, String role, List<String> skills, List<String> interests, List<String> sought, int hours, String goal, String style, String zone, List<Integer> availability) {
   this(name, role, skills, interests, sought, hours, goal, style, zone, availability, "INDIVIDUAL", "COLLABORATOR", "", List.of());
  }
 }
 record ValidatedProfile(ProfileDraft profile, String accountType, boolean persisted) {}
 @PostMapping("/validate")
 ValidatedProfile validate(@Valid @RequestBody ProfileDraft profile) {
  try { java.time.ZoneId.of(profile.timezone()); }
  catch (java.time.DateTimeException ex) { throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid timezone"); }
  return new ValidatedProfile(profile, "DEMO", false);
 }
}
