package tacos.security;

import static org.springframework.security.test.web.servlet
    .request.SecurityMockMvcRequestPostProcessors
    .csrf;
import static org.springframework.test.web.servlet
    .request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet
    .request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet
    .request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet
    .result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet
    .WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails
    .UserDetailsService;
import org.springframework.security.test.context.support
    .WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.test.context.ContextConfiguration;

@WebMvcTest(controllers = AuthorizationMatrixTest.TestEndpoints.class)
@ContextConfiguration(
    classes = {
        SecurityConfig.class,
        AuthorizationMatrixTest.TestEndpoints.class
    })
public class AuthorizationMatrixTest {
  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private UserDetailsService
      userDetailsService;

  @Test
  public void anonymousCanReadCatalog()
      throws Exception {

    mockMvc.perform(get("/api/tacos/1").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
  }

  @Test
  public void anonymousCannotReadOrders()
      throws Exception {

    mockMvc.perform(
            get("/api/orders")
                .accept(
                    MediaType.APPLICATION_JSON))
        .andExpect(
            status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "USER")
  public void userCanReadOrders()
      throws Exception {

    mockMvc.perform(
            get("/api/orders"))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "USER")
  public void userCannotCreateIngredient()
      throws Exception {

    mockMvc.perform(
            post("/api/ingredients")
                .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  public void adminCanCreateIngredient()
      throws Exception {

    mockMvc.perform(
            post("/api/ingredients")
                .with(csrf()))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "USER")
  public void userCannotUpdateCatalog()
      throws Exception {

    mockMvc.perform(patch("/api/admin/ingredients/FLTO/catalog").with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  public void adminCanUpdateCatalog()
      throws Exception {

    mockMvc.perform(patch("/api/admin/ingredients/FLTO/catalog").with(csrf()))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "USER")
  public void userCannotAccessKitchen()
      throws Exception {

    mockMvc.perform(
            get("/api/kitchen/queue"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "KITCHEN")
  public void kitchenCanAccessKitchen()
      throws Exception {

    mockMvc.perform(
            get("/api/kitchen/queue"))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "USER")
  public void unknownRouteIsDenied()
      throws Exception {

    mockMvc.perform(
            get("/api/not-listed"))
        .andExpect(status().isForbidden());
  }

  @Test
  public void healthIsPublic()
      throws Exception {

    mockMvc.perform(
            get("/actuator/health"))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "USER")
  public void userCannotAccessActuatorInfo()
      throws Exception {

    mockMvc.perform(
            get("/actuator/info"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  public void adminCanAccessActuatorInfo()
      throws Exception {

    mockMvc.perform(
            get("/actuator/info"))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "USER")
  public void userCannotAccessDataRest()
      throws Exception {

    mockMvc.perform(
            get("/data-api/ingredients"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  public void adminCanAccessDataRest()
      throws Exception {

    mockMvc.perform(
            get("/data-api/ingredients"))
        .andExpect(status().isOk());
  }

  @Test
  public void anonymousCannotTokenizePayment()
        throws Exception {

      mockMvc.perform(
              post("/api/payment-methods/tokenize")
                  .with(csrf()))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void userCanTokenizePayment()
        throws Exception {

      mockMvc.perform(
              post("/api/payment-methods/tokenize")
                  .with(csrf()))
          .andExpect(status().isOk());
    }

    @Test
    public void registrationRequiresCsrfToken()
        throws Exception {

      mockMvc.perform(post("/register").contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden());
    }

    @Test
    public void anonymousCanRegisterWithCsrfHeader()
        throws Exception {

      mockMvc.perform(post("/register").with(csrf().asHeader()).contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isOk());
    }

    @Test
    public void publicEndpointProvidesAngularCsrfToken()
        throws Exception {

      mockMvc.perform(get("/csrf"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
          .andExpect(jsonPath("$.token").isNotEmpty());
    }

  @RestController
  static class TestEndpoints {

    @GetMapping("/api/tacos/1")
    public String taco() {
      return "taco";
    }

    @GetMapping("/api/orders")
    public String orders() {
      return "orders";
    }

    @PostMapping("/api/ingredients")
    public String createIngredient() {
      return "created";
    }

    @org.springframework.web.bind.annotation.PatchMapping("/api/admin/ingredients/FLTO/catalog")
    public String updateCatalog() {
      return "updated";
    }

    @GetMapping("/api/kitchen/queue")
    public String kitchen() {
      return "kitchen";
    }

    @GetMapping("/api/not-listed")
    public String unknown() {
      return "unknown";
    }

    @GetMapping("/actuator/health")
    public String health() {
      return "UP";
    }

    @GetMapping("/actuator/info")
    public String actuatorInfo() {
      return "info";
    }

    @GetMapping("/data-api/ingredients")
    public String dataRest() {
      return "ingredients";
    }

    @PostMapping("/api/payment-methods/tokenize")
    public String tokenizePayment() {
      return "tokenized";
    }

    @GetMapping("/register")
    public String registrationForm() {
      return "register";
    }

    @PostMapping("/register")
    public String register() {
      return "registered";
    }

    @GetMapping("/csrf")
    public org.springframework.security.web.csrf.CsrfToken csrf(
        org.springframework.security.web.csrf.CsrfToken token) {
      return token;
    }
  }
}
