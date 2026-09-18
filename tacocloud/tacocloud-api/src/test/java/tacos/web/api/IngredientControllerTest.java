package tacos.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;
import tacos.Ingredient;
import tacos.Ingredient.Type;
import tacos.data.IngredientRepository;
import tacos.web.api.dto.IngredientRequest;
import tacos.web.api.dto.IngredientResponse;
import tacos.web.api.error.GlobalApiExceptionHandler;
import tacos.web.api.mapper.IngredientMapper;

public class IngredientControllerTest {

  @Test
  public void shouldUpdateIngredient() {
    IngredientRepository repo =
        Mockito.mock(IngredientRepository.class);

    Ingredient existingIngredient =
        domainIngredient(
            "1L",
            "Original Ingredient",
            Type.WRAP);

    Mockito.when(repo.findById("1L"))
        .thenReturn(
            Mono.just(existingIngredient));

    Mockito.when(
            repo.save(
                Mockito.any(Ingredient.class)))
        .thenAnswer(invocation -> {
          Ingredient saved =
              invocation.getArgument(
                  0,
                  Ingredient.class);

          return Mono.just(saved);
        });

    WebTestClient testClient =
        testClient(repo);

    IngredientRequest request =
        ingredientRequest(
            "Updated Ingredient",
            Type.SAUCE);

    testClient.put()
        .uri("/api/ingredients/1L")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus().isOk()
        .expectBody(IngredientResponse.class)
        .value(response -> {
          assertEquals(
              "1L",
              response.getId());

          assertEquals(
              "Updated Ingredient",
              response.getName());

          assertEquals(
              Type.SAUCE,
              response.getType());
        });

    Mockito.verify(repo)
        .findById("1L");

    Mockito.verify(repo)
        .save(existingIngredient);
  }

  @Test
  public void shouldReturnNotFoundForUpdateIngredient() {
    IngredientRepository repo =
        Mockito.mock(IngredientRepository.class);

    Mockito.when(repo.findById("1L"))
        .thenReturn(Mono.empty());

    WebTestClient testClient =
        testClient(repo);

    testClient.put()
        .uri("/api/ingredients/1L")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
            ingredientRequest(
                "Updated Ingredient",
                Type.WRAP))
        .exchange()
        .expectStatus().isNotFound();

    Mockito.verify(repo)
        .findById("1L");

    Mockito.verify(
        repo,
        Mockito.never())
        .save(
            Mockito.any(Ingredient.class));
  }

  @Test
  public void shouldRejectIdInUpdateBody() {
    IngredientRepository repo =
        Mockito.mock(IngredientRepository.class);

    WebTestClient testClient =
        testClient(repo);

    testClient.put()
        .uri("/api/ingredients/1L")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(
            "{"
                + "\"id\":\"2L\","
                + "\"name\":\"Updated Ingredient\","
                + "\"type\":\"WRAP\""
                + "}")
        .exchange()
        .expectStatus().isBadRequest();

    Mockito.verifyNoInteractions(repo);
  }

  @Test
  public void shouldDeleteIngredient() {
    IngredientRepository repo =
        Mockito.mock(IngredientRepository.class);

    Ingredient ingredient =
        domainIngredient(
            "1L",
            "Test Ingredient",
            Type.WRAP);

    Mockito.when(repo.findById("1L"))
        .thenReturn(Mono.just(ingredient));

    Mockito.when(repo.delete(ingredient))
        .thenReturn(Mono.empty());

    WebTestClient testClient =
        testClient(repo);

    testClient.delete()
        .uri("/api/ingredients/1L")
        .exchange()
        .expectStatus().isNoContent()
        .expectBody()
        .isEmpty();

    Mockito.verify(repo)
        .findById("1L");

    Mockito.verify(repo)
        .delete(ingredient);
  }

  @Test
  public void shouldReturnNotFoundForDeleteIngredient() {
    IngredientRepository repo =
        Mockito.mock(IngredientRepository.class);

    Mockito.when(repo.findById("1L"))
        .thenReturn(Mono.empty());

    WebTestClient testClient =
        testClient(repo);

    testClient.delete()
        .uri("/api/ingredients/1L")
        .exchange()
        .expectStatus().isNotFound();

    Mockito.verify(repo)
        .findById("1L");

    Mockito.verify(
        repo,
        Mockito.never())
        .delete(
            Mockito.any(Ingredient.class));
  }

  @Test
  public void shouldCreateIngredient() {
    IngredientRepository repo =
        Mockito.mock(IngredientRepository.class);

    Mockito.when(
            repo.save(
                Mockito.any(Ingredient.class)))
        .thenAnswer(invocation -> {
          Ingredient entity =
              invocation.getArgument(
                  0,
                  Ingredient.class);

          /*
           * El request no asignó el ID.
           * Simulamos que persistencia lo genera.
           */
          assertNull(entity.getId());

          entity.setId("1L");

          return Mono.just(entity);
        });

    WebTestClient testClient =
        testClient(repo);

    IngredientRequest request =
        ingredientRequest(
            "Test Ingredient",
            Type.WRAP);

    testClient.post()
        .uri(
            "https://api.example.com:8443"
                + "/api/ingredients")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .exchange()
        .expectStatus().isCreated()
        .expectHeader()
        .valueEquals(
            "Location",
            "https://api.example.com:8443"
                + "/api/ingredients/1L")
        .expectBody(IngredientResponse.class)
        .value(response -> {
          assertEquals(
              "1L",
              response.getId());

          assertEquals(
              "Test Ingredient",
              response.getName());

          assertEquals(
              Type.WRAP,
              response.getType());
        });

    Mockito.verify(repo)
        .save(
            Mockito.any(Ingredient.class));
  }

  @Test
  public void shouldReturnBadRequestForCreateIngredient() {
    IngredientRepository repo =
        Mockito.mock(IngredientRepository.class);

    WebTestClient testClient =
        testClient(repo);

    IngredientRequest invalidRequest =
        new IngredientRequest();

    testClient.post()
        .uri(
            "https://api.example.com:8443"
                + "/api/ingredients")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(invalidRequest)
        .exchange()
        .expectStatus().isBadRequest();

    Mockito.verifyNoInteractions(repo);
  }

  private WebTestClient testClient(
      IngredientRepository repo) {

    IngredientMapper mapper =
        new IngredientMapper();

    IngredientController controller =
        new IngredientController(
            repo,
            mapper);

    return WebTestClient
        .bindToController(controller)
        .controllerAdvice(new GlobalApiExceptionHandler())
        .build();
  }

  private IngredientRequest ingredientRequest(
      String name,
      Type type) {

    IngredientRequest request =
        new IngredientRequest();

    request.setName(name);
    request.setType(type);

    return request;
  }

  private Ingredient domainIngredient(
      String id,
      String name,
      Type type) {

    return new Ingredient(
        id,
        name,
        type);
  }
}