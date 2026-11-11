package com.teamforge;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
class RecommendationFeedbackController {
 private static final String SESSION_KEY="recommendationFeedbackOffers";
 private final CollaborationController collaboration;
 private final DecisionLookup decisions;
 private final JdbcTemplate sql;
 private final ObjectMapper json;
 private final TransactionTemplate tx;
 RecommendationFeedbackController(CollaborationController c,DecisionLookup d,JdbcTemplate sql,ObjectMapper json,PlatformTransactionManager manager) {
  collaboration=c; decisions=d; this.sql=sql; this.json=json; tx=new TransactionTemplate(manager);
 }
 // Only feedback for an actual issued recommendation is accepted. Bounded, session-scoped
 // opaque offers contain no profile text, email, timezone, or selected availability.
 static String offer(HttpSession session,UUID actor,UUID target,int compatibility,String modelVersion,Map<?,?> features) {
  if (features==null || modelVersion==null || !List.of("weighted-reciprocal-v2","weighted-reciprocal-v2.1").contains(modelVersion)) return null;
  var safe=new LinkedHashMap<String,Double>();
  for (String key:List.of("roles","skills","interests","availability","commitment","goal","style")) {
   Object v=features.get(key);
   if (!(v instanceof Number n) || !Double.isFinite(n.doubleValue()) || n.doubleValue()<0 || n.doubleValue()>100) return null;
   safe.put(key,n.doubleValue());
  }
  var offers=new LinkedHashMap<String,Object>();
  Object previous=session.getAttribute(SESSION_KEY);
  if (previous instanceof Map<?,?> entries) entries.forEach((k,v)->offers.put(k.toString(),v));
  while (offers.size()>=100) offers.remove(offers.keySet().iterator().next());
  String token=UUID.randomUUID().toString();
  var entry=new LinkedHashMap<String,Object>();
  entry.put("actor",actor.toString()); entry.put("target",target.toString()); entry.put("score",compatibility);
  entry.put("model",modelVersion); entry.put("features",safe); entry.put("expires",Instant.now().plusSeconds(1800).toEpochMilli());
  offers.put(token,entry); session.setAttribute(SESSION_KEY,offers); return token;
 }
 record Feedback(@NotBlank @Size(max=36) String token,@NotBlank @Pattern(regexp="RELEVANT|NOT_RELEVANT") String rating) {}
 @PostMapping("/api/discovery/feedback") @ResponseStatus(HttpStatus.NO_CONTENT)
 void save(@Valid @RequestBody Feedback feedback,Authentication auth,HttpSession session) {
  UUID actor=collaboration.owner(auth);
  Object raw=session.getAttribute(SESSION_KEY);
  if (!(raw instanceof Map<?,?> offers) || !(offers.get(feedback.token()) instanceof Map<?,?> entry)
      || !actor.toString().equals(entry.get("actor")) || !(entry.get("expires") instanceof Number expires) || expires.longValue()<Instant.now().toEpochMilli()) throw error(409);
  UUID target=UUID.fromString(entry.get("target").toString());
  tx.executeWithoutResult(s->{
   var ids=new ArrayList<>(List.of(actor,target)); ids.sort(Comparator.comparing(UUID::toString));
   for (UUID id:ids) sql.queryForList("SELECT id FROM accounts WHERE id=? FOR UPDATE",id);
   if (actor.equals(target) || decisions.blocked(actor,target) || sql.queryForObject("SELECT COUNT(*) FROM profiles WHERE account_id=? AND discoverable=true",Long.class,target)==0) throw error(404);
   try {
    sql.update("DELETE FROM recommendation_feedback WHERE actor_id=? AND target_id=?",actor,target);
    sql.update("INSERT INTO recommendation_feedback(actor_id,target_id,rating,model_version,compatibility,features_json,updated_at) VALUES(?,?,?,?,?,?,?)",actor,target,feedback.rating(),entry.get("model"),entry.get("score"),json.writeValueAsString(entry.get("features")),Timestamp.from(Instant.now()));
   } catch (com.fasterxml.jackson.core.JsonProcessingException ex) { throw error(500); }
  });
 }
 @DeleteMapping("/api/account/recommendation-feedback") @ResponseStatus(HttpStatus.NO_CONTENT)
 void clear(Authentication auth) { sql.update("DELETE FROM recommendation_feedback WHERE actor_id=?",collaboration.owner(auth)); }
 private ResponseStatusException error(int status) { return new ResponseStatusException(HttpStatus.valueOf(status)); }
}
