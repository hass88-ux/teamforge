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
  var cookie=new DefaultCookieSerializer();
  cookie.setCookieName("JSESSIONID"); cookie.setCookiePath("/");
  cookie.setUseHttpOnlyCookie(true); cookie.setUseSecureCookie(true); cookie.setSameSite("Lax");
  return cookie;
 }
}
