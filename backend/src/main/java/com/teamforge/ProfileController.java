package com.teamforge;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController @RequestMapping("/api/profiles/me")
class ProfileController {
 private final AccountRepository accounts;
 private final ProfileRepository profiles;
 private final ObjectMapper json;
 ProfileController(AccountRepository accounts, ProfileRepository profiles, ObjectMapper json) { this.accounts=accounts; this.profiles=profiles; this.json=json; }
 record ProfileView(OnboardingController.ProfileDraft profile,String accountType,boolean persisted,boolean discoverable) {}
 @PutMapping @Transactional
 ProfileView save(@Valid @RequestBody OnboardingController.ProfileDraft draft, Authentication auth) throws JsonProcessingException {
  new OnboardingController().validate(draft);
  var owner=accounts.findByEmail(auth.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
  var profile=profiles.findById(owner.id).orElseGet(() -> new StoredProfile(owner.id));
  profile.profileJson=json.writeValueAsString(draft); profile.updatedAt=Instant.now(); profiles.saveAndFlush(profile);
  return new ProfileView(draft,"REAL",true,profile.discoverable);
 }
 record Visibility(@jakarta.validation.constraints.NotNull Boolean discoverable) {}
 @PutMapping("/visibility") @Transactional
 ProfileView visibility(@Valid @RequestBody Visibility request, Authentication auth) throws JsonProcessingException {
  var owner=accounts.findByEmail(auth.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
  var profile=profiles.findById(owner.id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  profile.discoverable=request.discoverable(); profile.updatedAt=Instant.now(); profiles.saveAndFlush(profile);
  return new ProfileView(json.readValue(profile.profileJson,OnboardingController.ProfileDraft.class),"REAL",true,profile.discoverable);
 }
 @GetMapping
 ProfileView get(Authentication auth) throws JsonProcessingException {
  var owner=accounts.findByEmail(auth.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
  var profile=profiles.findById(owner.id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  return new ProfileView(json.readValue(profile.profileJson,OnboardingController.ProfileDraft.class),"REAL",true,profile.discoverable);
 }
}
