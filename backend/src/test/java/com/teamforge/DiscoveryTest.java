package com.teamforge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class DiscoveryTest {
 static final ObjectMapper JSON=new ObjectMapper();
 static HttpServer server;
 static volatile String received;
 static volatile boolean fail;
 static {
  try {
   server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
   server.createContext("/recommendations/real", exchange -> {
    received=new String(exchange.getRequestBody().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
    if (fail) { exchange.sendResponseHeaders(500,-1); exchange.close(); return; }
    var request=JSON.readTree(received); var result=JSON.createObjectNode().put("accountType","REAL");
    var recommendations=result.putArray("recommendations");
    for (var candidate:request.get("candidates")) {
     var item=recommendations.addObject(); item.set("candidate",candidate); item.put("compatibility",80);
     item.put("forwardScore",80); item.put("reverseScore",80); item.putArray("evidence").add("Shared project interests: Education.");
     item.putObject("contributions").put("interests",20); item.put("modelVersion","test");
    }
    byte[] bytes=JSON.writeValueAsBytes(result); exchange.getResponseHeaders().set("Content-Type","application/json");
    exchange.sendResponseHeaders(200,bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
   }); server.start();
  } catch (Exception ex) { throw new RuntimeException(ex); }
 }
 @DynamicPropertySource static void ai(DynamicPropertyRegistry registry) { registry.add("teamforge.ai-url",() -> "http://127.0.0.1:"+server.getAddress().getPort()); }
 @Autowired MockMvc http;
 @Autowired AccountRepository accounts;
 @Autowired ProfileRepository profiles;
 private Account owner;
 private String draft(String name) { return """
 {"displayName":"%s","role":"Backend engineer","skills":["Java"],"interests":["Education"],"rolesSought":["Frontend engineer"],"weeklyHours":8,"goal":"Startup","workingStyle":"Structured","timezone":"UTC","availability":[20]}
 """.formatted(name); }
 private Account account(String name,boolean visible) {
  var account=new Account(UUID.randomUUID()+"@example.test","hashed-password"); accounts.saveAndFlush(account);
  var profile=new StoredProfile(account.id); profile.profileJson=draft(name); profile.updatedAt=Instant.now(); profile.discoverable=visible;
  profiles.saveAndFlush(profile); return account;
 }
 @BeforeEach void setup() { fail=false; received=null; profiles.deleteAll(); accounts.deleteAll(); owner=account("Owner",false); }
 @AfterAll static void stop() { server.stop(0); }
 @Test void visibilityDefaultsPrivateAndNeedsAuthenticatedCsrf() throws Exception {
  http.perform(get("/api/profiles/me").with(user(owner.email))).andExpect(jsonPath("$.discoverable").value(false));
  http.perform(get("/api/discovery/recommendations")).andExpect(status().isUnauthorized());
  http.perform(put("/api/profiles/me/visibility").with(user(owner.email)).contentType(MediaType.APPLICATION_JSON).content("{\"discoverable\":true}")).andExpect(status().isForbidden());
  http.perform(put("/api/profiles/me/visibility").with(user(owner.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
 }
 @Test void optInCanBeReversedAndSurvivesProfileEdits() throws Exception {
  http.perform(put("/api/profiles/me/visibility").with(user(owner.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"discoverable\":true}")).andExpect(status().isOk()).andExpect(jsonPath("$.discoverable").value(true));
  http.perform(put("/api/profiles/me").with(user(owner.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(draft("Edited"))).andExpect(jsonPath("$.discoverable").value(true));
  http.perform(put("/api/profiles/me/visibility").with(user(owner.email)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"discoverable\":false}")).andExpect(jsonPath("$.discoverable").value(false));
  assertThat(profiles.findById(owner.id).orElseThrow().discoverable).isFalse();
 }
 @Test void excludesSelfAndPrivateProfilesAndStripsPrivateSignals() throws Exception {
  var hidden=account("Private",false); var visible=account("Visible",true);
  var self=profiles.findById(owner.id).orElseThrow(); self.discoverable=true; profiles.saveAndFlush(self);
  var result=http.perform(get("/api/discovery/recommendations").with(user(owner.email))).andExpect(status().isOk()).andExpect(jsonPath("$.accountType").value("REAL")).andExpect(jsonPath("$.recommendations.length()").value(1)).andExpect(jsonPath("$.recommendations[0].candidate.id").value(visible.id.toString())).andReturn();
  String body=result.getResponse().getContentAsString(); assertThat(body).doesNotContain("availability","timezone","password",visible.email,hidden.id.toString());
  var sent=JSON.readTree(received); assertThat(sent.get("candidates").size()).isEqualTo(1);
  assertThat(sent.get("candidates").get(0).get("id").asText()).isEqualTo(visible.id.toString());
  assertThat(sent.get("profile").get("id").asText()).isEqualTo(owner.id.toString());
 }
 @Test void emptyNetworkNeverFabricatesCandidatesAndAiFailureIsRetryable() throws Exception {
  http.perform(get("/api/discovery/recommendations").with(user(owner.email))).andExpect(status().isOk()).andExpect(jsonPath("$.recommendations.length()").value(0));
  assertThat(received).isNull();
  account("Visible",true); fail=true;
  http.perform(get("/api/discovery/recommendations").with(user(owner.email))).andExpect(status().isServiceUnavailable());
 }
}
