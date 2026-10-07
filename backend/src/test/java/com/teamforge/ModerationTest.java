package com.teamforge;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="teamforge.moderator-ids=00000000-0000-0000-0000-000000000001") @AutoConfigureMockMvc
class ModerationTest {
 @Autowired MockMvc http;
 @Autowired JdbcTemplate sql;
 final UUID admin=UUID.fromString("00000000-0000-0000-0000-000000000001");
 UUID reporter,target,match,report;
 @BeforeEach void setup() {
  sql.update("DELETE FROM accounts");
  reporter=UUID.randomUUID(); target=UUID.randomUUID(); match=UUID.randomUUID(); report=UUID.randomUUID();
  for (UUID id:List.of(admin,reporter,target)) sql.update("INSERT INTO accounts(id,email,password_hash,account_type,created_at) VALUES(?,?,?,?,?)",id,id+"@example.test","hashed","REAL",Timestamp.from(Instant.now()));
  sql.update("INSERT INTO collaboration_matches(id,member_a,member_b,compatibility,created_at) VALUES(?,?,?,?,?)",match,reporter,target,90,Timestamp.from(Instant.now()));
  sql.update("INSERT INTO safety_reports(id,reporter_id,match_id,client_id,reason,details,created_at) VALUES(?,?,?,?,?,?,?)",report,reporter,match,UUID.randomUUID(),"Harassment","Please review",Timestamp.from(Instant.now()));
 }
 @Test void privateQueueRequiresExplicitAccountIdAndNeverReturnsEmails() throws Exception {
  http.perform(get("/api/moderation/reports")).andExpect(status().isUnauthorized());
  http.perform(get("/api/moderation/reports").with(user(reporter+"@example.test"))).andExpect(status().isForbidden());
  http.perform(get("/api/moderation/access").with(user(reporter+"@example.test"))).andExpect(jsonPath("$.allowed").value(false));
  var response=http.perform(get("/api/moderation/reports").with(user(admin+"@example.test"))).andExpect(status().isOk()).andExpect(jsonPath("$.reports[0].details").value("Please review")).andReturn();
  assertThat(response.getResponse().getContentAsString()).doesNotContain("@example.test","password_hash");
 }
 @Test void closureIsAuditedRevokesMessagingAndRejectsStaleRepeat() throws Exception {
  String path="/api/moderation/reports/"+report+"/review";
  String body="{\"revision\":0,\"outcome\":\"MATCH_CLOSED\",\"note\":\"Confirmed unwanted contact\"}";
  http.perform(post(path).with(user(admin+"@example.test")).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
  http.perform(post(path).with(user(reporter+"@example.test")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
  http.perform(post(path).with(user(admin+"@example.test")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
  assertThat(sql.queryForObject("SELECT closed_at FROM collaboration_matches WHERE id=?",Object.class,match)).isNotNull();
  assertThat(sql.queryForObject("SELECT reviewed_by FROM safety_reports WHERE id=?",UUID.class,report)).isEqualTo(admin);
  http.perform(get("/api/matches/"+match+"/messages").with(user(reporter+"@example.test"))).andExpect(status().isNotFound());
  http.perform(post(path).with(user(admin+"@example.test")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
 }
 @Test void dismissKeepsMatchOpenAndValidatesQueueAndNotes() throws Exception {
  http.perform(get("/api/moderation/reports?offset=-1").with(user(admin+"@example.test"))).andExpect(status().isBadRequest());
  String path="/api/moderation/reports/"+report+"/review";
  http.perform(post(path).with(user(admin+"@example.test")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"revision\":0,\"outcome\":\"DISMISSED\",\"note\":\" \"}")).andExpect(status().isBadRequest());
  http.perform(post(path).with(user(admin+"@example.test")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"revision\":0,\"outcome\":\"DISMISSED\",\"note\":\"Insufficient evidence\"}")).andExpect(status().isOk());
  assertThat(sql.queryForObject("SELECT closed_at FROM collaboration_matches WHERE id=?",Object.class,match)).isNull();
  http.perform(get("/api/moderation/reports?status=DISMISSED").with(user(admin+"@example.test"))).andExpect(jsonPath("$.reports[0].review_note").value("Insufficient evidence"));
 }
}
