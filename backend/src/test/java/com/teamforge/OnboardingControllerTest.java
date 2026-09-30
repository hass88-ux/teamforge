package com.teamforge;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import static org.assertj.core.api.Assertions.assertThat;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class OnboardingControllerTest {
 @Autowired TestRestTemplate http;
 private ResponseEntity<String> submit(String body) {
  var headers = new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_JSON);
  return http.postForEntity("/api/onboarding/validate", new HttpEntity<>(body, headers), String.class);
 }
 private String valid() { return """
 {"displayName":"Builder","role":"Backend","skills":["Java"],"interests":["Education"],"rolesSought":["Frontend"],"weeklyHours":8,"goal":"Portfolio project","workingStyle":"Structured","timezone":"America/New_York","availability":[20,44]}
 """; }
 @Test void validatesWithoutPersistingOrCreatingRealAccount() {
  var result=submit(valid());
  assertThat(result.getStatusCode().value()).isEqualTo(200);
  assertThat(result.getBody()).contains("\"accountType\":\"DEMO\"", "\"persisted\":false");
 }
 @Test void rejectsMissingSignals() { assertThat(submit("{}").getStatusCode().value()).isEqualTo(400); }
 @Test void rejectsInvalidTimezone() { assertThat(submit(valid().replace("America/New_York","Invalid/Zone")).getStatusCode().value()).isEqualTo(400); }
 @Test void rejectsOutOfRangeAvailability() { assertThat(submit(valid().replace("[20,44]","[168]")).getStatusCode().value()).isEqualTo(400); }
}
