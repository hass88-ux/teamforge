package com.teamforge;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Bean;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;
import org.springframework.session.web.http.DefaultCookieSerializer;

// Flyway owns the schema; local development retains servlet sessions.
@Configuration
@Profile("production")
@EnableJdbcHttpSession(maxInactiveIntervalInSeconds = 1800, cleanupCron = "0 */10 * * * *")
class PersistentSessionConfiguration {
 @Bean DefaultCookieSerializer cookieSerializer() {
  var cookie=new DefaultCookieSerializer() {
   @Override public java.util.List<String> readCookieValues(jakarta.servlet.http.HttpServletRequest request) {
    // Legacy Tomcat IDs can decode as binary (including NUL), which PostgreSQL
    // cannot accept as text. Only generated UUID session IDs may reach JDBC.
    try { return super.readCookieValues(request).stream().filter(id -> id.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")).toList(); }
    catch (IllegalArgumentException ex) { return java.util.List.of(); }
   }
  };
  cookie.setCookieName("JSESSIONID"); cookie.setCookiePath("/");
  cookie.setUseHttpOnlyCookie(true); cookie.setUseSecureCookie(true); cookie.setSameSite("Lax");
  return cookie;
 }
}
