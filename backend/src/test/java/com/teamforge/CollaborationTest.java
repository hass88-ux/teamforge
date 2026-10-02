package com.teamforge;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class CollaborationTest {
 @Autowired MockMvc http;
 @Autowired AccountRepository accounts;
 @Autowired ProfileRepository profiles;
 @Autowired JdbcTemplate sql;
 @Autowired ObjectMapper json;
 @Autowired CollaborationController controller;
 @MockitoBean DiscoveryController ranking;
 Account a,b,outside;
 private Account account(String name,String intent) {
  var account=accounts.saveAndFlush(new Account(UUID.randomUUID()+"@example.test","hashed"));
  var p=new StoredProfile(account.id); p.discoverable=true; p.updatedAt=Instant.now();
  p.profileJson="""
   {"displayName":"%s","role":"Backend engineer","skills":["Java"],"interests":["Education"],"rolesSought":["Backend engineer"],"weeklyHours":8,"goal":"Startup","workingStyle":"Structured","timezone":"UTC","availability":[20],"matchingIntent":"%s"}
   """.formatted(name,intent); profiles.saveAndFlush(p); return account;
 }
 @BeforeEach void setup() {
  sql.update("DELETE FROM collaboration_messages"); sql.update("DELETE FROM collaboration_matches"); sql.update("DELETE FROM profile_decisions"); profiles.deleteAll(); accounts.deleteAll();
  a=account("Alice","PROVIDER"); b=account("Bob","SEEKER"); outside=account("Outside","COLLABORATOR");
  when(ranking.compatibility(any(),any())).thenReturn(90);
 }
 private String vote(Account target,String decision) { return "{\"candidateId\":\""+target.id+"\",\"decision\":\""+decision+"\"}"; }
 private String message(UUID id,String text) { return "{\"clientId\":\""+id+"\",\"text\":\""+text+"\"}"; }
 private UUID match() throws Exception {
  http.perform(post("/api/discovery/decisions").with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(vote(b,"LIKE"))).andExpect(status().isOk()).andExpect(jsonPath("$.matched").value(false));
  var response=http.perform(post("/api/discovery/decisions").with(user(b.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(vote(a,"LIKE"))).andExpect(status().isOk()).andExpect(jsonPath("$.matched").value(true)).andReturn();
  return UUID.fromString(json.readTree(response.getResponse().getContentAsString()).get("matchId").asText());
 }
 @Test void requiresGenuineMutualLikesAndPersistsPasses() throws Exception {
  http.perform(post("/api/discovery/decisions").with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(vote(b,"PASS"))).andExpect(status().isOk()).andExpect(jsonPath("$.matched").value(false));
  assertThat(sql.queryForObject("SELECT decision FROM profile_decisions WHERE actor_id=? AND target_id=?",String.class,a.id,b.id)).isEqualTo("PASS");
  UUID id=match();
  http.perform(get("/api/matches").with(user(a.email))).andExpect(jsonPath("$[0].id").value(id.toString())).andExpect(jsonPath("$[0].displayName").value("Bob"));
  http.perform(post("/api/discovery/decisions").with(user(b.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(vote(a,"LIKE"))).andExpect(jsonPath("$.matchId").value(id.toString()));
  assertThat(sql.queryForObject("SELECT COUNT(*) FROM collaboration_matches",Long.class)).isEqualTo(1);
 }
 @Test void validatesIntentVisibilitySelfAndThreshold() throws Exception {
  http.perform(post("/api/discovery/decisions").with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(vote(a,"LIKE"))).andExpect(status().isBadRequest());
  var other=account("Provider","PROVIDER");
  http.perform(post("/api/discovery/decisions").with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(vote(other,"LIKE"))).andExpect(status().isConflict());
  var hidden=profiles.findById(b.id).orElseThrow(); hidden.discoverable=false; hidden=profiles.saveAndFlush(hidden);
  http.perform(post("/api/discovery/decisions").with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(vote(b,"LIKE"))).andExpect(status().isNotFound());
  hidden.discoverable=true; profiles.saveAndFlush(hidden); when(ranking.compatibility(any(),any())).thenReturn(50);
  http.perform(post("/api/discovery/decisions").with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(vote(b,"LIKE"))).andExpect(status().isConflict());
  assertThat(sql.queryForObject("SELECT COUNT(*) FROM profile_decisions",Long.class)).isZero();
 }
 @Test void outsiderCannotReadOrSendAndWritesNeedCsrf() throws Exception {
  UUID id=match();
  http.perform(get("/api/matches")).andExpect(status().isUnauthorized());
  http.perform(get("/api/matches/"+id+"/messages").with(user(outside.email))).andExpect(status().isNotFound());
  http.perform(post("/api/matches/"+id+"/messages").with(user(outside.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(message(UUID.randomUUID(),"Hi"))).andExpect(status().isNotFound());
  http.perform(post("/api/matches/"+id+"/messages").with(user(a.email)).contentType(MediaType.APPLICATION_JSON).content(message(UUID.randomUUID(),"Hi"))).andExpect(status().isForbidden());
  http.perform(get("/api/matches").with(user(outside.email))).andExpect(jsonPath("$.length()").value(0));
 }
 @Test void retriesDoNotDuplicateMessagesAndOnlyMembersReceiveThem() throws Exception {
  UUID id=match(),client=UUID.randomUUID(); String path="/api/matches/"+id+"/messages";
  for (int i=0;i<2;i++) http.perform(post(path).with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(message(client,"Hello Bob"))).andExpect(status().isOk()).andExpect(jsonPath("$.fromYou").value(true));
  http.perform(post(path).with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(message(client,"Changed text"))).andExpect(status().isConflict());
  http.perform(get(path).with(user(b.email))).andExpect(jsonPath("$.messages.length()").value(1)).andExpect(jsonPath("$.messages[0].fromYou").value(false)).andExpect(jsonPath("$.messages[0].text").value("Hello Bob"));
  assertThat(sql.queryForObject("SELECT COUNT(*) FROM collaboration_messages",Long.class)).isEqualTo(1);
 }
 @Test void unmatchClosesAccessAndPreventsFurtherContact() throws Exception {
  UUID id=match();
  http.perform(delete("/api/matches/"+id).with(user(outside.email)).with(csrf())).andExpect(status().isNotFound());
  http.perform(delete("/api/matches/"+id).with(user(a.email)).with(csrf())).andExpect(status().isNoContent());
  http.perform(get("/api/matches/"+id+"/messages").with(user(b.email))).andExpect(status().isNotFound());
  http.perform(post("/api/matches/"+id+"/messages").with(user(b.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(message(UUID.randomUUID(),"Hi"))).andExpect(status().isNotFound());
  http.perform(post("/api/discovery/decisions").with(user(b.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(vote(a,"LIKE"))).andExpect(status().isConflict());
 }
 @Test void simultaneousReciprocalLikesCreateExactlyOneMatch() throws Exception {
  var barrier=new CyclicBarrier(2);
  when(ranking.compatibility(any(),any())).thenAnswer(call -> { barrier.await(5,TimeUnit.SECONDS); return 90; });
  try (var executor=Executors.newFixedThreadPool(2)) {
   var left=executor.submit(() -> controller.decide(new CollaborationController.Decision(b.id,"LIKE"),new UsernamePasswordAuthenticationToken(a.email,"",List.of())));
   var right=executor.submit(() -> controller.decide(new CollaborationController.Decision(a.id,"LIKE"),new UsernamePasswordAuthenticationToken(b.email,"",List.of())));
   left.get(10,TimeUnit.SECONDS); right.get(10,TimeUnit.SECONDS);
  }
  assertThat(sql.queryForObject("SELECT COUNT(*) FROM collaboration_matches",Long.class)).isEqualTo(1);
  assertThat(sql.queryForObject("SELECT COUNT(*) FROM profile_decisions",Long.class)).isEqualTo(2);
 }
 @Test void rejectsBlankOversizeAndRateLimitedMessages() throws Exception {
  UUID id=match(); String path="/api/matches/"+id+"/messages";
  for (String text:List.of("   ","a".repeat(1001))) http.perform(post(path).with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(message(UUID.randomUUID(),text))).andExpect(status().isBadRequest());
  for (int i=0;i<30;i++) http.perform(post(path).with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(message(UUID.randomUUID(),"Hello"))).andExpect(status().isOk());
  http.perform(post(path).with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(message(UUID.randomUUID(),"Hello"))).andExpect(status().isTooManyRequests());
 }
 @Test void paginatesMessagesAndRejectsProfileChangesDuringScoring() throws Exception {
  UUID id=match();
  for (int i=0;i<51;i++) sql.update("INSERT INTO collaboration_messages(match_id,sender_id,client_id,body,created_at) VALUES(?,?,?,?,?)",id,a.id,UUID.randomUUID(),"Message "+i,java.sql.Timestamp.from(Instant.now()));
  var result=http.perform(get("/api/matches/"+id+"/messages").with(user(b.email))).andExpect(jsonPath("$.messages.length()").value(50)).andExpect(jsonPath("$.hasMore").value(true)).andReturn();
  long after=json.readTree(result.getResponse().getContentAsString()).get("nextAfter").asLong();
  http.perform(get("/api/matches/"+id+"/messages?after="+after).with(user(b.email))).andExpect(jsonPath("$.messages.length()").value(1)).andExpect(jsonPath("$.hasMore").value(false));
  var newCandidate=account("New candidate","SEEKER");
  when(ranking.compatibility(any(),any())).thenAnswer(call -> { var p=profiles.findById(newCandidate.id).orElseThrow(); p.discoverable=false; profiles.saveAndFlush(p); return 90; });
  http.perform(post("/api/discovery/decisions").with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(vote(newCandidate,"LIKE"))).andExpect(status().isConflict());
  assertThat(sql.queryForObject("SELECT COUNT(*) FROM profile_decisions WHERE target_id=?",Long.class,newCandidate.id)).isZero();
 }

 private String proposal(UUID client,Instant time) { return "{\"clientId\":\""+client+"\",\"kind\":\"Virtual coffee\",\"startsAt\":\""+time+"\",\"timezone\":\"America/New_York\",\"note\":\"Discuss Java\"}"; }
 @Test void proposalsRequireMembershipCsrfAndValidFutureTimes() throws Exception {
  UUID id=match(); String path="/api/matches/"+id+"/proposals";
  http.perform(get(path).with(user(outside.email))).andExpect(status().isNotFound());
  http.perform(post(path).with(user(a.email)).contentType(MediaType.APPLICATION_JSON).content(proposal(UUID.randomUUID(),Instant.now().plusSeconds(3600)))).andExpect(status().isForbidden());
  for (Instant time:List.of(Instant.now().minusSeconds(60),Instant.now().plusSeconds(181L*86400))) http.perform(post(path).with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(proposal(UUID.randomUUID(),time))).andExpect(status().isBadRequest());
  http.perform(post(path).with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(proposal(UUID.randomUUID(),Instant.now().plusSeconds(3600)).replace("America/New_York","Invalid/Zone"))).andExpect(status().isBadRequest());
 }
 @Test void proposalRetriesAndResponseConsentAreEnforced() throws Exception {
  UUID id=match(),client=UUID.randomUUID(); String path="/api/matches/"+id+"/proposals"; String body=proposal(client,Instant.now().plusSeconds(3600));
  var saved=http.perform(post(path).with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andReturn();
  String proposalId=json.readTree(saved.getResponse().getContentAsString()).get("id").asText();
  http.perform(post(path).with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(jsonPath("$.id").value(proposalId));
  http.perform(post(path).with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body.replace("Discuss Java","Different note"))).andExpect(status().isConflict());
  String responsePath=path+"/"+proposalId+"/response";
  http.perform(post(responsePath).with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACCEPTED\"}")).andExpect(status().isForbidden());
  http.perform(post(responsePath).with(user(b.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACCEPTED\"}")).andExpect(jsonPath("$.status").value("ACCEPTED"));
  http.perform(post(responsePath).with(user(b.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DECLINED\"}")).andExpect(status().isConflict());
  http.perform(post(responsePath).with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELLED\"}")).andExpect(jsonPath("$.status").value("CANCELLED"));
  http.perform(get(path).with(user(b.email))).andExpect(jsonPath("$[0].status").value("CANCELLED")).andExpect(jsonPath("$[0].fromYou").value(false));
 }
 @Test void proposalAccessEndsOnUnmatchAndPendingPoolIsBounded() throws Exception {
  UUID id=match(); String path="/api/matches/"+id+"/proposals";
  for (int i=0;i<10;i++) http.perform(post(path).with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(proposal(UUID.randomUUID(),Instant.now().plusSeconds(3600)))).andExpect(status().isOk());
  http.perform(post(path).with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(proposal(UUID.randomUUID(),Instant.now().plusSeconds(3600)))).andExpect(status().isTooManyRequests());
  sql.update("UPDATE coffee_proposals SET starts_at=? WHERE id=(SELECT id FROM coffee_proposals WHERE match_id=? LIMIT 1)",java.sql.Timestamp.from(Instant.now().minusSeconds(60)),id);
  http.perform(post(path).with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(proposal(UUID.randomUUID(),Instant.now().plusSeconds(3600)))).andExpect(status().isOk());
  http.perform(delete("/api/matches/"+id).with(user(a.email)).with(csrf())).andExpect(status().isNoContent());
  http.perform(get(path).with(user(b.email))).andExpect(status().isNotFound());
 }
 private String project(UUID client,String name) { return "{\"clientId\":\""+client+"\",\"name\":\""+name+"\",\"description\":\"Build a learning tool\",\"stage\":\"Idea\"}"; }
 @Test void projectsRequireOwnershipConsentAndActiveMatches() throws Exception {
  UUID match=match(), client=UUID.randomUUID(); String draft=project(client,"Learning team");
  http.perform(get("/api/projects")).andExpect(status().isUnauthorized());
  http.perform(post("/api/projects").with(user(a.email)).contentType(MediaType.APPLICATION_JSON).content(draft)).andExpect(status().isForbidden());
  var created=http.perform(post("/api/projects").with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(draft)).andExpect(status().isOk()).andExpect(jsonPath("$.yourStatus").value("OWNER")).andReturn();
  UUID id=UUID.fromString(json.readTree(created.getResponse().getContentAsString()).get("id").asText());
  http.perform(post("/api/projects").with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(draft)).andExpect(jsonPath("$.id").value(id.toString()));
  http.perform(post("/api/projects").with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(project(client,"Changed"))).andExpect(status().isConflict());
  http.perform(get("/api/projects").with(user(outside.email))).andExpect(jsonPath("$.length()").value(0));
  String invite="{\"matchId\":\""+match+"\"}";
  http.perform(post("/api/projects/"+id+"/members").with(user(b.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(invite)).andExpect(status().isNotFound());
  http.perform(post("/api/projects/"+id+"/members").with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(invite)).andExpect(status().isOk()).andExpect(jsonPath("$.members.length()").value(2));
  http.perform(post("/api/projects/"+id+"/members").with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(invite)).andExpect(jsonPath("$.members.length()").value(2));
  http.perform(get("/api/projects").with(user(b.email))).andExpect(jsonPath("$[0].yourStatus").value("INVITED"));
  http.perform(post("/api/projects/"+id+"/response").with(user(outside.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACCEPTED\"}")).andExpect(status().isNotFound());
  http.perform(post("/api/projects/"+id+"/response").with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACCEPTED\"}")).andExpect(status().isForbidden());
  http.perform(post("/api/projects/"+id+"/response").with(user(b.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACCEPTED\"}")).andExpect(jsonPath("$.yourStatus").value("ACCEPTED"));
  http.perform(post("/api/projects/"+id+"/response").with(user(b.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DECLINED\"}")).andExpect(status().isConflict());
  http.perform(delete("/api/matches/"+match).with(user(a.email)).with(csrf())).andExpect(status().isNoContent());
  http.perform(post("/api/projects/"+id+"/members").with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(invite)).andExpect(status().isNotFound());
  http.perform(get("/api/projects").with(user(b.email))).andExpect(jsonPath("$[0].yourStatus").value("ACCEPTED"));
 }
 @Test void declinedProjectIsHiddenAndProjectLimitIsEnforced() throws Exception {
  UUID match=match();
  var created=http.perform(post("/api/projects").with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(project(UUID.randomUUID(),"First"))).andReturn();
  String id=json.readTree(created.getResponse().getContentAsString()).get("id").asText();
  String invite="{\"matchId\":\""+match+"\"}";
  http.perform(post("/api/projects/"+id+"/members").with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(invite)).andExpect(status().isOk());
  http.perform(post("/api/projects/"+id+"/response").with(user(b.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DECLINED\"}")).andExpect(status().isOk());
  http.perform(get("/api/projects").with(user(b.email))).andExpect(jsonPath("$.length()").value(0));
  http.perform(post("/api/projects/"+id+"/members").with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(invite)).andExpect(status().isConflict());
  for (int i=1;i<20;i++) http.perform(post("/api/projects").with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(project(UUID.randomUUID(),"Project "+i))).andExpect(status().isOk());
  http.perform(post("/api/projects").with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(project(UUID.randomUUID(),"Overflow"))).andExpect(status().isTooManyRequests());
  http.perform(post("/api/projects").with(user(b.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(project(UUID.randomUUID()," "))).andExpect(status().isBadRequest());
 }
 @Test void projectEditsUseOwnerAuthorizationAndRejectStaleVersions() throws Exception {
  var saved=http.perform(post("/api/projects").with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(project(UUID.randomUUID(),"Original"))).andReturn();
  String id=json.readTree(saved.getResponse().getContentAsString()).get("id").asText();
  String edits="{\"name\":\"Updated\",\"description\":\"Build a prototype\",\"stage\":\"Prototype\",\"revision\":0}";
  http.perform(put("/api/projects/"+id).with(user(a.email)).contentType(MediaType.APPLICATION_JSON).content(edits)).andExpect(status().isForbidden());
  http.perform(put("/api/projects/"+id).with(user(outside.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(edits)).andExpect(status().isNotFound());
  http.perform(put("/api/projects/"+id).with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(edits)).andExpect(jsonPath("$.name").value("Updated")).andExpect(jsonPath("$.revision").value(1));
  http.perform(put("/api/projects/"+id).with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(edits)).andExpect(status().isConflict());
  http.perform(get("/api/projects").with(user(a.email))).andExpect(jsonPath("$[0].stage").value("Prototype"));
 }
 @Test void leavingAndRemovalEndProjectAccessAndCannotRestoreMembership() throws Exception {
  UUID match=match();
  var saved=http.perform(post("/api/projects").with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(project(UUID.randomUUID(),"Team"))).andReturn();
  String id=json.readTree(saved.getResponse().getContentAsString()).get("id").asText(), base="/api/projects/"+id;
  http.perform(post(base+"/members").with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"matchId\":\""+match+"\"}")).andExpect(status().isOk());
  http.perform(post(base+"/leave").with(user(a.email)).with(csrf())).andExpect(status().isForbidden());
  http.perform(post(base+"/leave").with(user(outside.email)).with(csrf())).andExpect(status().isNotFound());
  http.perform(post(base+"/members/"+b.id+"/remove").with(user(b.email)).with(csrf())).andExpect(status().isNotFound());
  http.perform(post(base+"/members/"+a.id+"/remove").with(user(a.email)).with(csrf())).andExpect(status().isBadRequest());
  http.perform(post(base+"/members/"+b.id+"/remove").with(user(a.email)).with(csrf())).andExpect(jsonPath("$.members[1].status").value("REMOVED"));
  http.perform(post(base+"/response").with(user(b.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACCEPTED\"}")).andExpect(status().isNotFound());
  http.perform(get("/api/projects").with(user(b.email))).andExpect(jsonPath("$.length()").value(0));
  http.perform(post(base+"/members").with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"matchId\":\""+match+"\"}")).andExpect(status().isConflict());
  var second=http.perform(post("/api/projects").with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(project(UUID.randomUUID(),"Second"))).andReturn();
  base="/api/projects/"+json.readTree(second.getResponse().getContentAsString()).get("id").asText();
  http.perform(post(base+"/members").with(user(a.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"matchId\":\""+match+"\"}")).andExpect(status().isOk());
  http.perform(post(base+"/response").with(user(b.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACCEPTED\"}")).andExpect(status().isOk());
  http.perform(post(base+"/leave").with(user(b.email))).andExpect(status().isForbidden());
  http.perform(post(base+"/leave").with(user(b.email)).with(csrf())).andExpect(status().isNoContent());
  http.perform(post(base+"/leave").with(user(b.email)).with(csrf())).andExpect(status().isNotFound());
  http.perform(get("/api/projects").with(user(b.email))).andExpect(jsonPath("$.length()").value(0));
 }
}
