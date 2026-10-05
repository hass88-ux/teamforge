package com.teamforge;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class ReleaseRoutingTest {
 @Autowired MockMvc http;
 @Test void publicStaticRoutesDoNotExposePrivateApiOrArbitraryFiles() throws Exception {
  http.perform(get("/api/account/export")).andExpect(status().isUnauthorized());
  http.perform(get("/application.properties")).andExpect(status().isUnauthorized());
  http.perform(get("/.local/teamforge.mv.db")).andExpect(status().isUnauthorized());
  http.perform(post("/index.html")).andExpect(status().isForbidden());
 }
}
