package com.teamforge;

import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class RecommendationFeedbackTest {
 @Autowired MockMvc http;
 @Autowired AccountRepository accounts;
 @Autowired ProfileRepository profiles;
 @Autowired JdbcTemplate sql;
 Account actor,target,other;
 MockHttpSession session;
 String token;
 static final Map<String,Double> FEATURES=Map.of("roles",25.,"skills",20.,"interests",20.,"availability",15.,"commitment",10.,"goal",5.,"style",5.);
 private Account account() {
  var a=accounts.saveAndFlush(new Account(UUID.randomUUID()+"@example.test","hashed"));
  var p=new StoredProfile(a.id); p.discoverable=true; p.updatedAt=Instant.now();
  p.profileJson="{}"; profiles.saveAndFlush(p); return a;
 }
 @BeforeEach void setup() {
  actor=account(); target=account(); other=account(); session=new MockHttpSession();
  token=RecommendationFeedbackController.offer(session,actor.id,target.id,90,"weighted-reciprocal-v2.1",FEATURES);
 }
 @AfterEach void cleanup() { for (var a:List.of(actor,target,other)) accounts.deleteById(a.id); }
 private String body(String t,String rating) { return "{\"token\":\""+t+"\",\"rating\":\""+rating+"\"}"; }
 private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder vote(String rating) {
  return post("/api/discovery/feedback").session(session).with(user(actor.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(token,rating));
 }
 @Test void ratingIsIndependentUpdatableExportableAndClearable() throws Exception {
  http.perform(vote("RELEVANT")).andExpect(status().isNoContent());
  http.perform(vote("NOT_RELEVANT")).andExpect(status().isNoContent());
  assertThat(sql.queryForObject("SELECT COUNT(*) FROM recommendation_feedback WHERE actor_id=?",Long.class,actor.id)).isEqualTo(1);
  assertThat(sql.queryForObject("SELECT rating FROM recommendation_feedback WHERE actor_id=?",String.class,actor.id)).isEqualTo("NOT_RELEVANT");
  assertThat(sql.queryForObject("SELECT COUNT(*) FROM profile_decisions WHERE actor_id=?",Long.class,actor.id)).isZero();
  http.perform(get("/api/account/export").with(user(actor.email))).andExpect(status().isOk()).andExpect(jsonPath("$.recommendationFeedback[0].rating").value("NOT_RELEVANT"));
  http.perform(get("/api/account/export").with(user(other.email))).andExpect(jsonPath("$.recommendationFeedback").isEmpty());
  http.perform(delete("/api/account/recommendation-feedback").with(user(other.email)).with(csrf())).andExpect(status().isNoContent());
  assertThat(sql.queryForObject("SELECT COUNT(*) FROM recommendation_feedback WHERE actor_id=?",Long.class,actor.id)).isEqualTo(1);
  http.perform(delete("/api/account/recommendation-feedback").with(user(actor.email)).with(csrf())).andExpect(status().isNoContent());
  assertThat(sql.queryForObject("SELECT COUNT(*) FROM recommendation_feedback WHERE actor_id=?",Long.class,actor.id)).isZero();
 }
 @Test void requiresAuthenticationCsrfAndIssuedSession() throws Exception {
  http.perform(post("/api/discovery/feedback").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(token,"RELEVANT"))).andExpect(status().isUnauthorized());
  http.perform(post("/api/discovery/feedback").session(session).with(user(actor.email)).contentType(MediaType.APPLICATION_JSON).content(body(token,"RELEVANT"))).andExpect(status().isForbidden());
  http.perform(post("/api/discovery/feedback").with(user(actor.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(token,"RELEVANT"))).andExpect(status().isConflict());
  http.perform(post("/api/discovery/feedback").session(session).with(user(other.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(token,"RELEVANT"))).andExpect(status().isConflict());
  http.perform(vote("LIKE")).andExpect(status().isBadRequest());
 }
 @Test void rejectsWithdrawnBlockedAndGeneratedTargets() throws Exception {
  sql.update("UPDATE profiles SET discoverable=false WHERE account_id=?",target.id);
  http.perform(vote("RELEVANT")).andExpect(status().isNotFound());
  sql.update("UPDATE profiles SET discoverable=true WHERE account_id=?",target.id);
  sql.update("INSERT INTO account_blocks(actor_id,target_id,created_at) VALUES(?,?,?)",target.id,actor.id,java.sql.Timestamp.from(Instant.now()));
  http.perform(vote("RELEVANT")).andExpect(status().isNotFound());
  token=RecommendationFeedbackController.offer(session,actor.id,UUID.randomUUID(),90,"weighted-reciprocal-v2.1",FEATURES);
  http.perform(vote("RELEVANT")).andExpect(status().isNotFound());
 }
 @Test void capsOffersAndRejectsExpiredOrFabricatedTokens() throws Exception {
  for (int i=0;i<101;i++) RecommendationFeedbackController.offer(session,actor.id,target.id,90,"weighted-reciprocal-v2.1",FEATURES);
  http.perform(vote("RELEVANT")).andExpect(status().isConflict());
  var expired=new LinkedHashMap<String,Object>();
  expired.put("actor",actor.id.toString()); expired.put("target",target.id.toString()); expired.put("expires",0L);
  session.setAttribute("recommendationFeedbackOffers",new LinkedHashMap<>(Map.of(token,expired)));
  http.perform(vote("RELEVANT")).andExpect(status().isConflict());
  token=UUID.randomUUID().toString(); http.perform(vote("RELEVANT")).andExpect(status().isConflict());
 }
}
