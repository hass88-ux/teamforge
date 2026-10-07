package com.teamforge;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
 "spring.profiles.active=production", "spring.datasource.url=jdbc:h2:mem:persistent_sessions;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
 "spring.datasource.username=sa", "spring.datasource.password=", "spring.datasource.driver-class-name=org.h2.Driver"
})
class PersistentSessionTest {
 @LocalServerPort int port;
 @Autowired JdbcTemplate sql;
 @Autowired PlatformTransactionManager manager;
 @Autowired ObjectMapper json;
 @Autowired org.springframework.session.web.http.DefaultCookieSerializer cookies;
 private final HttpClient client=HttpClient.newHttpClient();
 record Login(String cookie,String id,String email) {}
 HttpResponse<String> request(String path,String method,String body,String cookie,String token) throws Exception {
  var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path));
  if (cookie!=null) builder.header("Cookie",cookie);
  if (token!=null) builder.header("X-CSRF-TOKEN",token);
  if (body!=null) builder.header("Content-Type","application/json");
  return client.send(builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
 }
 String csrf(String cookie) throws Exception { return json.readTree(request("/api/auth/csrf","GET",null,cookie,null).body()).path("token").asText(); }
 Login signup() throws Exception {
  var initial=request("/api/auth/csrf","GET",null,null,null);
  String cookie=initial.headers().firstValue("set-cookie").orElseThrow().split(";")[0];
  String email="session-"+UUID.randomUUID()+"@example.test";
  var response=request("/api/auth/signup","POST",json.writeValueAsString(Map.of("email",email,"password","Correct horse 42!")),cookie,json.readTree(initial.body()).path("token").asText());
  assertThat(response.statusCode()).isEqualTo(201);
  String header=response.headers().firstValue("set-cookie").orElseThrow();
  assertThat(header).contains("Secure","HttpOnly","SameSite=Lax");
  cookie=header.split(";")[0];
  String id=new String(Base64.getDecoder().decode(cookie.substring(cookie.indexOf('=')+1)),java.nio.charset.StandardCharsets.UTF_8);
  return new Login(cookie,id,email);
 }
 @Test void databaseSessionCanBeLoadedByANewRepositoryAndLogoutRevokesIt() throws Exception {
  var login=signup();
  var fresh=new JdbcIndexedSessionRepository(sql,new TransactionTemplate(manager));
  org.springframework.session.Session stored=fresh.findById(login.id());
  assertThat(stored).isNotNull(); assertThat((Object)stored.getAttribute("SPRING_SECURITY_CONTEXT")).isNotNull();
  assertThat(request("/api/auth/me","GET",null,login.cookie(),null).statusCode()).isEqualTo(200);
  assertThat(request("/api/auth/logout","POST",null,login.cookie(),null).statusCode()).isEqualTo(403);
  assertThat(request("/api/auth/logout","POST",null,login.cookie(),csrf(login.cookie())).statusCode()).isEqualTo(204);
  assertThat(fresh.findById(login.id())).isNull();
  assertThat(request("/api/auth/me","GET",null,login.cookie(),null).statusCode()).isEqualTo(401);
 }
 @Test void changedCredentialsInvalidatePersistedSessionsOnTheirNextRequest() throws Exception {
  var login=signup();
  sql.update("UPDATE accounts SET password_hash=? WHERE email=?","revoked-test-credential",login.email());
  assertThat(request("/api/auth/me","GET",null,login.cookie(),null).statusCode()).isEqualTo(401);
  assertThat(sql.queryForObject("SELECT COUNT(*) FROM spring_session WHERE session_id=?",Long.class,login.id())).isZero();
 }
 @Test void expiredDatabaseSessionCannotAuthenticate() throws Exception {
  var login=signup();
  sql.update("UPDATE spring_session SET last_access_time=0,expiry_time=0 WHERE session_id=?",login.id());
  assertThat(request("/api/auth/me","GET",null,login.cookie(),null).statusCode()).isEqualTo(401);
 }
 @Test void accountDeletionRemovesPersistedAuthenticationData() throws Exception {
  var login=signup();
  assertThat(sql.queryForObject("SELECT COUNT(*) FROM spring_session WHERE principal_name=?",Long.class,login.email())).isEqualTo(1);
  assertThat(request("/api/account/delete","POST","{\"password\":\"Correct horse 42!\"}",login.cookie(),csrf(login.cookie())).statusCode()).isEqualTo(204);
  assertThat(sql.queryForObject("SELECT COUNT(*) FROM spring_session WHERE principal_name=?",Long.class,login.email())).isZero();
  assertThat(sql.queryForObject("SELECT COUNT(*) FROM spring_session_attributes WHERE session_primary_id NOT IN (SELECT primary_id FROM spring_session)",Long.class)).isZero();
  assertThat(request("/api/auth/me","GET",null,login.cookie(),null).statusCode()).isEqualTo(401);
 }
 @Test void legacyAndMalformedCookiesNeverReachDatabaseSessionLookup() throws Exception {
  for (String value:List.of("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "not-a-cookie", Base64.getEncoder().encodeToString(new byte[36]))) {
   var request=new org.springframework.mock.web.MockHttpServletRequest();
   request.setCookies(new jakarta.servlet.http.Cookie("JSESSIONID",value));
   assertThat(cookies.readCookieValues(request)).isEmpty();
   assertThat(request("/api/auth/me","GET",null,"JSESSIONID="+value,null).statusCode()).isEqualTo(401);
   assertThat(request("/api/auth/csrf","GET",null,"JSESSIONID="+value,null).statusCode()).isEqualTo(200);
  }
 }
}
