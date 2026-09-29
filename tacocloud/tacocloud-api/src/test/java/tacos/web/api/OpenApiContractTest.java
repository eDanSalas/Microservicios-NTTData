package tacos.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

public class OpenApiContractTest {

  private Map<String, Object> document;

  @BeforeEach
  public void setUp() {
    InputStream source = getClass().getResourceAsStream("/static/openapi.yaml");
    assertNotNull(source);
    document = new Yaml().load(source);
  }

  @Test
  public void shouldValidateOpenApiDocumentAndAllReferences() {
    assertEquals("3.0.3", document.get("openapi"));
    assertEquals("/api/v1", map(list(document, "servers").get(0)).get("url"));
    validateNode(document);
    map(document, "paths").forEach((path, value) -> map(value).forEach((method, operation) -> {
      if (!"parameters".equals(method)) assertTrue(map(operation).containsKey("responses"),
          path + " " + method + " must declare responses");
    }));
  }

  @Test
  public void shouldDocumentEveryImplementedEndpoint() {
    assertEquals(Set.of("/ingredients", "/ingredients/{id}", "/tacos", "/tacos/today",
        "/tacos/validate", "/tacos/top", "/tacos/{id}", "/tacos/{id}/classification",
        "/tacos/{id}/rating", "/orders", "/orders/fromEmail", "/orders/quote",
        "/orders/{orderId}", "/orders/{id}/status", "/orders/{id}/cancel",
        "/orders/{id}/reorder", "/users/me/favorites", "/users/me/favorites/{tacoId}",
        "/users/me/orders", "/users/me/orders/{id}", "/kitchen/queue",
        "/kitchen/orders/claim", "/kitchen/orders/{id}/status",
        "/payment-methods/tokenize", "/announcements", "/admin/announcements",
        "/admin/announcements/{id}", "/admin/ingredients/{id}/catalog",
        "/admin/ingredients/{id}/stock-adjustments", "/admin/orders"),
        map(document, "paths").keySet());
    Set<String> operations = new java.util.HashSet<>();
    map(document, "paths").forEach((path, value) -> map(value).forEach((method, operation) -> {
      if (!"parameters".equals(method)) operations.add(method.toUpperCase() + " " + path);
    }));
    assertEquals(Set.of("GET /ingredients", "POST /ingredients", "GET /ingredients/{id}",
        "PUT /ingredients/{id}", "DELETE /ingredients/{id}", "GET /tacos", "POST /tacos",
        "GET /tacos/today", "POST /tacos/validate", "GET /tacos/top", "GET /tacos/{id}",
        "GET /tacos/{id}/classification", "GET /tacos/{id}/rating", "PUT /tacos/{id}/rating",
        "POST /orders", "POST /orders/fromEmail", "POST /orders/quote",
        "PATCH /orders/{orderId}", "PUT /orders/{orderId}", "DELETE /orders/{orderId}",
        "PATCH /orders/{id}/status", "POST /orders/{id}/cancel", "POST /orders/{id}/reorder",
        "GET /users/me/favorites", "PUT /users/me/favorites/{tacoId}",
        "DELETE /users/me/favorites/{tacoId}", "GET /users/me/orders",
        "GET /users/me/orders/{id}", "GET /kitchen/queue", "POST /kitchen/orders/claim",
        "PATCH /kitchen/orders/{id}/status", "POST /payment-methods/tokenize",
        "GET /announcements", "POST /admin/announcements", "DELETE /admin/announcements/{id}",
        "PATCH /admin/ingredients/{id}/catalog", "POST /admin/ingredients/{id}/stock-adjustments",
        "GET /admin/orders"), operations);
  }

  @Test
  public void shouldMatchPostOrderAndApiProblemContract() {
    Map<String, Object> post = map(map(map(document, "paths"), "/orders"), "post");
    Map<String, Object> responses = map(post, "responses");
    assertTrue(responses.keySet().containsAll(Set.of("201", "400", "401", "409", "422")));
    assertEquals("#/components/schemas/OrderCreateRequest", map(map(map(post, "requestBody"),
        "content"), "application/json", "schema").get("$ref"));
    Map<String, Object> schemas = map(map(document, "components"), "schemas");
    assertTrue(list(map(schemas, "OrderResponse"), "required").containsAll(List.of(
        "id", "status", "userId", "total", "currency", "items")));
    assertEquals(List.of("type", "title", "status", "detail", "instance", "code", "violations"),
        list(map(schemas, "ApiProblem"), "required"));
  }

  @Test
  public void shouldNotExposeSensitiveFieldsInResponseSchemas() {
    Set<String> forbidden = Set.of("pan", "securityCode", "password", "ccNumber", "cvv");
    map(map(document, "components"), "schemas").forEach((name, schema) -> {
      if (name.endsWith("Response")) assertFalse(propertyNames(map(schema)).stream()
          .anyMatch(forbidden::contains), name + " exposes a sensitive field");
    });
  }

  private Set<String> propertyNames(Map<String, Object> schema) {
    if (schema.containsKey("properties")) return map(schema, "properties").keySet();
    return Set.of();
  }

  private void validateNode(Object node) {
    if (node instanceof Map) map(node).forEach((key, value) -> {
      if ("$ref".equals(key)) assertNotNull(resolve(value.toString()), "Unresolved ref: " + value);
      else validateNode(value);
    });
    if (node instanceof List) list(node).forEach(this::validateNode);
  }

  private Object resolve(String ref) {
    Object current = document;
    for (String part : ref.substring(2).split("/")) current = map(current).get(part);
    return current;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> map(Object value) {
    return (Map<String, Object>) value;
  }

  private Map<String, Object> map(Map<String, Object> value, String key) {
    return map(value.get(key));
  }

  private Map<String, Object> map(Map<String, Object> value, String first, String second) {
    return map(map(value, first), second);
  }

  @SuppressWarnings("unchecked")
  private List<Object> list(Object value) {
    return (List<Object>) value;
  }

  private List<Object> list(Map<String, Object> value, String key) {
    return list(value.get(key));
  }
}
