package com.teamforge;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/discovery")
class DiscoveryController {
 private final AccountRepository accounts;
 private final ProfileRepository profiles;
 private final ObjectMapper json;
 private final RestClient ai;
 private final DecisionLookup decisions;
 private static final List<String> PUBLIC_FIELDS = List.of("displayName", "role", "skills", "interests", "rolesSought", "weeklyHours", "goal", "workingStyle", "entityType", "matchingIntent", "description", "neededSkills");
 DiscoveryController(AccountRepository accounts, ProfileRepository profiles, ObjectMapper json, DecisionLookup decisions, @Value("${teamforge.ai-url:http://127.0.0.1:8001}") String url) {
  this.accounts=accounts; this.profiles=profiles; this.json=json; this.decisions=decisions;
  var factory=new SimpleClientHttpRequestFactory();
  factory.setConnectTimeout(Duration.ofSeconds(3)); factory.setReadTimeout(Duration.ofSeconds(10));
  ai=RestClient.builder().baseUrl(url).requestFactory(factory).build();
 }
 private Map<String,Object> signals(StoredProfile profile) {
  try {
   var draft=json.readValue(profile.profileJson, OnboardingController.ProfileDraft.class);
   Map<String,Object> result=json.convertValue(draft, new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>() {});
   result.put("id",profile.accountId.toString()); result.put("accountType","REAL"); return result;
  } catch (com.fasterxml.jackson.core.JsonProcessingException ex) { throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR); }
 }
 int compatibility(StoredProfile owner, StoredProfile candidate) {
  try {
   var response=ai.post().uri("/recommendations/real").body(Map.of("profile",signals(owner),"candidates",List.of(signals(candidate)))).retrieve().body(Map.class);
   if (response == null || !"REAL".equals(response.get("accountType")) || !(response.get("recommendations") instanceof List<?> items)) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
   if (items.isEmpty()) return 0;
   if (items.size()!=1 || !(items.getFirst() instanceof Map<?,?> item) || !(item.get("candidate") instanceof Map<?,?> person) || !candidate.accountId.toString().equals(person.get("id")) || !(item.get("compatibility") instanceof Number number) || !Double.isFinite(number.doubleValue()) || number.doubleValue()>100 || number.doubleValue()<=50) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
   return number.intValue();
  } catch (RestClientException ex) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Compatibility temporarily unavailable"); }
 }
 @GetMapping("/recommendations")
 Map<String,Object> recommendations(Authentication auth) {
  var owner=accounts.findByEmail(auth.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
  var ownProfile=profiles.findById(owner.id).orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,"Complete your profile first"));
  var page=profiles.findByDiscoverableTrueAndAccountIdNot(owner.id, PageRequest.of(0,200,Sort.by(Sort.Direction.DESC,"updatedAt").and(Sort.by("accountId"))));
  // Decisions are persistent; refresh does not reintroduce passed/liked profiles.
  var decided=decisions.targets(owner.id);
  var candidates=page.getContent().stream().filter(p -> !decided.contains(p.accountId)).map(this::signals).toList();
  if (candidates.isEmpty()) return Map.of("accountType","REAL","recommendations",List.of(),"searchedProfileCount",0,"poolLimited",false);
  Map<?,?> response;
  try { response=ai.post().uri("/recommendations/real").body(Map.of("profile",signals(ownProfile),"candidates",candidates)).retrieve().body(Map.class); }
  catch (RestClientException ex) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Recommendations temporarily unavailable"); }
  if (response == null || !"REAL".equals(response.get("accountType")) || !(response.get("recommendations") instanceof List<?> ranked)) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
  Map<String,Map<String,Object>> allowed=new HashMap<>();
  for (var candidate:candidates) allowed.put((String)candidate.get("id"),candidate);
  List<Map<String,Object>> result=new ArrayList<>();
  for (Object entry:ranked) {
   if (!(entry instanceof Map<?,?> item) || !(item.get("candidate") instanceof Map<?,?> candidate)) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
   var original=allowed.get(candidate.get("id"));
   if (original == null || !(item.get("compatibility") instanceof Number number) || number.doubleValue() <= 50) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
   // Recheck visibility after inference, so a withdrawn profile isn't returned from a stale snapshot.
   if (!profiles.findById(UUID.fromString((String)original.get("id"))).map(p -> p.discoverable).orElse(false) || decisions.blocked(owner.id,UUID.fromString((String)original.get("id")))) continue;
   Map<String,Object> publicProfile=new LinkedHashMap<>();
   publicProfile.put("id",original.get("id")); publicProfile.put("accountType","REAL");
   for (String field:PUBLIC_FIELDS) publicProfile.put(field,original.get(field));
   Map<String,Object> publicResult=new LinkedHashMap<>(); publicResult.put("candidate",publicProfile);
   for (String field:List.of("compatibility","forwardScore","reverseScore","evidence","contributions","modelVersion")) publicResult.put(field,item.get(field));
   result.add(publicResult);
  }
  return Map.of("accountType","REAL","recommendations",result,"searchedProfileCount",candidates.size(),"poolLimited",page.getTotalElements()>200);
 }
}
