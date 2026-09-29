package tacos;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource("classpath:taco-test.properties")
public class TacoCloudApplicationTests {

  @Autowired
  private MockMvc client;

  @Test
  public void contextLoads() {
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  public void invalidVersionedIngredientReturnsProblemDetails() throws Exception {
    client.perform(put("/api/v1/ingredients/FLTO").with(csrf())
        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\",\"type\":null}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType("application/problem+json"))
        .andExpect(header().exists("X-Correlation-Id"))
        .andExpect(jsonPath("$.type").value("urn:tacocloud:problem:validation-error"))
        .andExpect(jsonPath("$.title").value("Request validation failed"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").value("One or more fields are invalid"))
        .andExpect(jsonPath("$.instance").value("/api/v1/ingredients/FLTO"))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.violations.length()").value(3));
  }

}
