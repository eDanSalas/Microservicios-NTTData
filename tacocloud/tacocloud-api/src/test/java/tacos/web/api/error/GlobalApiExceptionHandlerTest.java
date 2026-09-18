package tacos.web.api.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;
import tacos.inventory.InsufficientStockException;
import tacos.web.api.dto.ApiProblem;
import tacos.web.api.dto.OrderCreateRequest;

public class GlobalApiExceptionHandlerTest {

  private WebTestClient testClient;

  @BeforeEach
  public void setUp() {
    testClient =
        WebTestClient
            .bindToController(new ErrorTestController())
            .controllerAdvice(new GlobalApiExceptionHandler())
            .build();
  }

  @Test
  public void shouldReportMultipleValidationErrors() {
    String invalidOrder =
        "{"
            + "\"deliveryName\":\"\","
            + "\"deliveryStreet\":\"\","
            + "\"deliveryCity\":\"Guadalajara\","
            + "\"deliveryState\":\"Jalisco\","
            + "\"deliveryZip\":\"ABC\","
            + "\"paymentMethodId\":\"\","
            + "\"items\":[]"
            + "}";

    testClient.post()
        .uri("/test/orders")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(invalidOrder)
        .exchange()
        .expectStatus().isBadRequest()
        .expectHeader()
        .contentType(
            MediaType.valueOf(
                "application/problem+json"))
        .expectBody(ApiProblem.class)
        .value(problem -> {
          assertEquals(
              400,
              problem.getStatus());

          assertEquals(
              "VALIDATION_ERROR",
              problem.getCode());

          assertEquals(
              "/test/orders",
              problem.getInstance().toString());

          Set<String> fields =
              problem.getViolations()
                  .stream()
                  .map(violation ->
                      violation.getField())
                  .collect(Collectors.toSet());

          assertTrue(
              fields.contains(
                  "deliveryName"));

          assertTrue(
              fields.contains(
                  "deliveryStreet"));

          assertTrue(
              fields.contains(
                  "deliveryZip"));

          assertTrue(
              fields.contains(
                  "paymentMethodId"));

          assertTrue(
              fields.contains(
                  "items"));
        });
  }

  @Test
  public void shouldMapBusinessErrorTo422() {
    testClient.get()
        .uri("/test/business")
        .exchange()
        .expectStatus()
        .isEqualTo(
            HttpStatus.UNPROCESSABLE_ENTITY)
        .expectHeader()
        .contentType(
            MediaType.valueOf(
                "application/problem+json"))
        .expectBody(ApiProblem.class)
        .value(problem -> {
          assertEquals(
              422,
              problem.getStatus());

          assertEquals(
              "BUSINESS_RULE_VIOLATION",
              problem.getCode());

          assertEquals(
              "Unknown ingredient id: BAD",
              problem.getDetail());
        });
  }

  @Test
  public void shouldMapConflictTo409() {
    testClient.get()
        .uri("/test/conflict")
        .exchange()
        .expectStatus().isEqualTo(
            HttpStatus.CONFLICT)
        .expectBody(ApiProblem.class)
        .value(problem -> {
          assertEquals(
              409,
              problem.getStatus());

          assertEquals(
              "RESOURCE_CONFLICT",
              problem.getCode());
        });
  }

  @Test
  public void shouldMapNotFoundAndIncludeInstance() {
    testClient.get()
        .uri("/test/missing")
        .exchange()
        .expectStatus().isNotFound()
        .expectBody(ApiProblem.class)
        .value(problem -> {
          assertEquals(
              "RESOURCE_NOT_FOUND",
              problem.getCode());

          assertEquals(
              "/test/missing",
              problem.getInstance().toString());
        });
  }

  @Test
  public void shouldMapInsufficientStockWithSpecificCode() {
    testClient.get()
        .uri("/test/stock")
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.CONFLICT)
        .expectBody(ApiProblem.class)
        .value(problem -> assertEquals("INSUFFICIENT_STOCK", problem.getCode()));
  }

  @Test
  public void shouldRejectMalformedJson() {
    testClient.post()
        .uri("/test/orders")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"deliveryName\":")
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody(ApiProblem.class)
        .value(problem -> {
          assertEquals(
              "MALFORMED_REQUEST",
              problem.getCode());
        });
  }

  @Test
  public void shouldNotExposeInternalInformation() {
    testClient.get()
        .uri("/test/internal")
        .exchange()
        .expectStatus().is5xxServerError()
        .expectHeader()
        .contentType(
            MediaType.valueOf(
                "application/problem+json"))
        .expectBody(String.class)
        .value(body -> {
          assertTrue(
              body.contains(
                  "\"code\":\"INTERNAL_ERROR\""));

          assertFalse(
              body.contains("MongoDB"));

          assertFalse(
              body.contains("com.mongodb"));

          assertFalse(
              body.contains("RuntimeException"));

          assertFalse(
              body.contains("stackTrace"));
        });
  }

  @RestController
  static class ErrorTestController {

    @PostMapping("/test/orders")
    public Mono<Void> validateOrder(
        @Valid @RequestBody
            OrderCreateRequest request) {

      return Mono.empty();
    }

    @GetMapping("/test/business")
    public Mono<Void> businessError() {
      return Mono.error(
          new ResponseStatusException(
              HttpStatus.UNPROCESSABLE_ENTITY,
              "Unknown ingredient id: BAD"));
    }

    @GetMapping("/test/conflict")
    public Mono<Void> conflict() {
      return Mono.error(
          new ResponseStatusException(
              HttpStatus.CONFLICT,
              "Order cannot be modified"));
    }

    @GetMapping("/test/stock")
    public Mono<Void> stock() {
      return Mono.error(new InsufficientStockException("FLTO"));
    }

    @GetMapping("/test/missing")
    public Mono<Void> missing() {
      return Mono.error(
          new ResponseStatusException(
              HttpStatus.NOT_FOUND,
              "Resource not found"));
    }

    @GetMapping("/test/internal")
    public Mono<Void> internalError() {
      return Mono.error(
          new RuntimeException(
              "MongoDB com.mongodb.DriverException"));
    }
  }
}
