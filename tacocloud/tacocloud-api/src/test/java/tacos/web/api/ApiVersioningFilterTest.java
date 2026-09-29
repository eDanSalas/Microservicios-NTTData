package tacos.web.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

public class ApiVersioningFilterTest {

  private MockMvc client;

  @BeforeEach
  public void setUp() {
    client = MockMvcBuilders.standaloneSetup(new ProbeController())
        .addFilters(new ApiVersioningFilter()).build();
  }

  @Test
  public void shouldServeVersionedAliasWithoutDeprecation() throws Exception {
    client.perform(get("/api/v1/probe").param("value", "ok"))
        .andExpect(status().isOk()).andExpect(header().doesNotExist("Deprecation"))
        .andExpect(content().string("ok"));
  }

  @Test
  public void shouldKeepLegacyAliasWithDeprecationHeaders() throws Exception {
    client.perform(get("/api/probe").param("value", "legacy"))
        .andExpect(status().isOk()).andExpect(header().string("Deprecation", "true"))
        .andExpect(header().string("Sunset", "Fri, 31 Dec 2027 23:59:59 GMT"))
        .andExpect(header().string("Link", "</api/v1/probe>; rel=\"successor-version\""))
        .andExpect(content().string("legacy"));
  }

  @RestController
  @RequestMapping("/api/probe")
  private static class ProbeController {

    @GetMapping
    public String probe(@RequestParam String value) {
      return value;
    }
  }
}
