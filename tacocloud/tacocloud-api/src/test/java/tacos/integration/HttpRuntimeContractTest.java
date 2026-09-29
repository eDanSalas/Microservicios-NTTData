package tacos.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoReactiveDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoReactiveRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import tacos.security.SecurityConfig;
import tacos.web.api.ApiVersioningFilter;
import tacos.observability.CorrelationIdWebFilter;
import tacos.web.api.error.ApiErrorController;
import tacos.web.api.error.ApiProblemFactory;
import tacos.web.api.error.ApiProblemSecurityHandler;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = HttpRuntimeContractTest.TestApplication.class)
class HttpRuntimeContractTest {

  @LocalServerPort
  private int port;
  private WebTestClient client;

  @BeforeEach
  void setUp() {
    client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
  }

  @Test
  void shouldServeVersionedPublicApiAndMarkLegacyRouteAsDeprecated() {
    client.get().uri("/api/v1/tacos/probe").exchange().expectStatus().isOk()
        .expectBody(String.class).isEqualTo("ready");
    client.get().uri("/api/tacos/probe").exchange().expectStatus().isOk()
        .expectHeader().valueEquals("Deprecation", "true")
        .expectHeader().exists("Sunset").expectHeader().exists("Link");
  }

  @Test
  void shouldRejectAnonymousAdminRequestOnRealServer() {
    client.get().uri("/api/v1/admin/probe").exchange()
        .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED)
        .expectHeader().contentType("application/problem+json")
        .expectHeader().exists("X-Correlation-Id").expectBody()
        .jsonPath("$.type").isEqualTo("urn:tacocloud:problem:authentication-required")
        .jsonPath("$.title").isEqualTo("Authentication required")
        .jsonPath("$.status").isEqualTo(401)
        .jsonPath("$.detail").isEqualTo("Authentication is required")
        .jsonPath("$.instance").isEqualTo("/api/v1/admin/probe")
        .jsonPath("$.code").isEqualTo("AUTHENTICATION_REQUIRED")
        .jsonPath("$.violations.length()").isEqualTo(0);
  }

  @Test
  void shouldReturnProblemDetailsForMissingPublicApiRoute() {
    client.get().uri("/api/v1/tacos/not-found").accept(MediaType.TEXT_HTML).exchange()
        .expectStatus().isNotFound()
        .expectHeader().contentType("application/problem+json")
        .expectHeader().exists("X-Correlation-Id").expectBody()
        .jsonPath("$.instance").isEqualTo("/api/v1/tacos/not-found")
        .jsonPath("$.code").isEqualTo("RESOURCE_NOT_FOUND")
        .jsonPath("$.violations.length()").isEqualTo(0);
  }

  @Test
  void shouldReturnProblemDetailsForForbiddenApiRequest() {
    client.get().uri("/api/v1/admin/probe").headers(headers ->
        headers.setBasicAuth("user", "password")).exchange().expectStatus().isForbidden()
        .expectHeader().contentType("application/problem+json")
        .expectHeader().exists("X-Correlation-Id").expectBody()
        .jsonPath("$.instance").isEqualTo("/api/v1/admin/probe")
        .jsonPath("$.code").isEqualTo("ACCESS_DENIED")
        .jsonPath("$.violations.length()").isEqualTo(0);
  }

  @Test
  void shouldAllowIdempotencyHeaderInCorsPreflight() {
    client.method(HttpMethod.OPTIONS).uri("/api/v1/orders")
        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Idempotency-Key")
        .exchange().expectStatus().isOk()
        .expectHeader().value(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
            value -> assertThat(value).containsIgnoringCase("Idempotency-Key"));
  }

  @SpringBootConfiguration
  @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, JmsAutoConfiguration.class,
      KafkaAutoConfiguration.class, MongoAutoConfiguration.class, MongoDataAutoConfiguration.class,
      MongoReactiveAutoConfiguration.class, MongoReactiveDataAutoConfiguration.class,
      MongoReactiveRepositoriesAutoConfiguration.class, EmbeddedMongoAutoConfiguration.class,
      RabbitAutoConfiguration.class})
  @Import({SecurityConfig.class, ApiVersioningFilter.class, CorrelationIdWebFilter.class,
      ApiProblemFactory.class, ApiProblemSecurityHandler.class, ApiErrorController.class,
      ProbeController.class})
  static class TestApplication {

    @Bean
    UserDetailsService userDetailsService() {
      return username -> {
        if ("user".equals(username)) return org.springframework.security.core.userdetails.User
            .withUsername(username).password("{noop}password").roles("USER").build();
        throw new UsernameNotFoundException(username);
      };
    }
  }

  @RestController
  static class ProbeController {

    @GetMapping({"/api/tacos/probe", "/api/admin/probe"})
    String probe() {
      return "ready";
    }
  }
}
