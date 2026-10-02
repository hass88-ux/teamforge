package com.teamforge;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DescriptionControllerTest {
 @Test void forwardsOnlyDescriptionAndValidatesLength() throws Exception {
  var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  var captured=new AtomicReference<String>();
  server.createContext("/description/skills",e->{
   captured.set(new String(e.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
   byte[] body="{\"offered\":[\"Java\"],\"needed\":[\"Python\"],\"unclassified\":[]}".getBytes(StandardCharsets.UTF_8);
   e.getResponseHeaders().set("Content-Type","application/json"); e.sendResponseHeaders(200,body.length); e.getResponseBody().write(body); e.close();
  }); server.start();
  try {
   var http=MockMvcBuilders.standaloneSetup(new DescriptionController("http://127.0.0.1:"+server.getAddress().getPort())).build();
   http.perform(post("/api/onboarding/skills").contentType(MediaType.APPLICATION_JSON).content("{\"description\":\"I offer Java and need Python\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.offered[0]").value("Java"));
   assertThat(captured.get()).isEqualTo("{\"description\":\"I offer Java and need Python\"}");
   for (String description:new String[]{"", "x".repeat(1001)}) http.perform(post("/api/onboarding/skills").contentType(MediaType.APPLICATION_JSON).content("{\"description\":\""+description+"\"}")).andExpect(status().isBadRequest());
  } finally { server.stop(0); }
 }
 @Test void unavailableServiceReturnsRetryableError() throws Exception {
  var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/description/skills",e->{e.sendResponseHeaders(503,-1); e.close();}); server.start();
  try {
   var http=MockMvcBuilders.standaloneSetup(new DescriptionController("http://127.0.0.1:"+server.getAddress().getPort())).build();
   http.perform(post("/api/onboarding/skills").contentType(MediaType.APPLICATION_JSON).content("{\"description\":\"Java\"}")).andExpect(status().isServiceUnavailable());
  } finally { server.stop(0); }
 }
}
