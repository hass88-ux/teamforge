package com.teamforge;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest @AutoConfigureMockMvc
class AuthProfileTest {
 @Autowired MockMvc http;
 @Autowired AccountRepository accounts;
 @Autowired ProfileRepository profiles;
 private static final String PASSWORD="Correct horse 42!";
 private String email() { return "builder-"+UUID.randomUUID()+"@example.test"; }
 private String credentials(String email,String password) { return "{\"email\":\""+email+"\",\"password\":\""+password+"\"}"; }
 private MockHttpSession signup(String email) throws Exception {
  var result=http.perform(post("/api/auth/signup").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(credentials(email,PASSWORD)))
   .andExpect(status().isCreated()).andExpect(jsonPath("$.accountType").value("REAL")).andReturn();
  assertThat(result.getResponse().getContentAsString()).doesNotContain(PASSWORD,"passwordHash");
  return (MockHttpSession)result.getRequest().getSession(false);
 }
 private String profile(String name) { return """
 {"displayName":"%s","role":"Backend engineer","skills":["Java"],"interests":["Education"],"rolesSought":["Frontend engineer"],"weeklyHours":8,"goal":"Portfolio project","workingStyle":"Structured","timezone":"UTC","availability":[20,44]}
 """.formatted(name); }
 @Test void publicDemoValidationNeverPersistsAccountsOrProfiles() throws Exception {
  long beforeAccounts=accounts.count(); long beforeProfiles=profiles.count();
  http.perform(post("/api/onboarding/validate").contentType(MediaType.APPLICATION_JSON).content(profile("Fictional demo")))
   .andExpect(status().isOk()).andExpect(jsonPath("$.accountType").value("DEMO")).andExpect(jsonPath("$.persisted").value(false));
  assertThat(accounts.count()).isEqualTo(beforeAccounts);
  assertThat(profiles.count()).isEqualTo(beforeProfiles);
 }
 @Test void signupRequiresCsrfAndHashesPasswords() throws Exception {
  String email=email();
  http.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(credentials(email,PASSWORD))).andExpect(status().isForbidden());
  var session=signup(email);
  http.perform(get("/api/auth/me").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.email").value(email));
  var account=accounts.findByEmail(email).orElseThrow(); assertThat(account.passwordHash).startsWith("$2a$12$").isNotEqualTo(PASSWORD);
 }
 @Test void loginRotatesSessionAndLogoutRemovesAccess() throws Exception {
  String email=email(); signup(email);
  var before=new MockHttpSession(); String oldId=before.getId();
  var login=http.perform(post("/api/auth/login").session(before).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(credentials(email,PASSWORD))).andExpect(status().isOk()).andReturn();
  var session=(MockHttpSession)login.getRequest().getSession(false); assertThat(session.getId()).isNotEqualTo(oldId);
  http.perform(get("/api/auth/me").session(session)).andExpect(status().isOk());
  http.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
  http.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
 }
 @Test void invalidLoginDoesNotRevealPasswordOrCreateAuthentication() throws Exception {
  String email=email(); signup(email);
  var result=http.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(credentials(email,"Wrong password 42!"))).andExpect(status().isUnauthorized()).andReturn();
  assertThat(result.getResponse().getContentAsString()).doesNotContain("Wrong password");
 }
 @Test void profilesArePersistentAndOwnedBySession() throws Exception {
  var owner=signup(email()); var other=signup(email());
  http.perform(put("/api/profiles/me").session(owner).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(profile("Owner"))).andExpect(status().isOk()).andExpect(jsonPath("$.persisted").value(true));
  http.perform(get("/api/profiles/me").session(owner)).andExpect(status().isOk()).andExpect(jsonPath("$.profile.displayName").value("Owner"));
  http.perform(get("/api/profiles/me").session(other)).andExpect(status().isNotFound());
  http.perform(put("/api/profiles/me").session(other).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(profile("Other"))).andExpect(status().isOk());
  http.perform(get("/api/profiles/me").session(owner)).andExpect(jsonPath("$.profile.displayName").value("Owner"));
  http.perform(get("/api/profiles/me")).andExpect(status().isUnauthorized());
 }
 @Test void protectedWritesNeedCsrfAndValidationDoesNotEchoSecrets() throws Exception {
  var session=signup(email());
  http.perform(put("/api/profiles/me").session(session).contentType(MediaType.APPLICATION_JSON).content(profile("Owner"))).andExpect(status().isForbidden());
  http.perform(post("/api/auth/signup").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(credentials(email(),"shortsecret"))).andExpect(status().isBadRequest()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("shortsecret"))));
 }
 @Test void exposesCsrfTokenForBrowserAndRejectsUtf8Truncation() throws Exception {
  http.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andExpect(jsonPath("$.token").isString()).andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"));
  http.perform(post("/api/auth/signup").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(credentials(email(),"é".repeat(40)))).andExpect(status().isBadRequest());
 }
}
