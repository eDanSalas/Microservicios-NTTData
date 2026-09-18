package tacos.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import tacos.Ingredient;
import tacos.Ingredient.Type;
import tacos.OrderItem;
import tacos.OrderStatus;
import tacos.Taco;
import tacos.TacoOrder;
import tacos.User;
import tacos.web.api.dto.IngredientRequest;
import tacos.web.api.dto.IngredientResponse;
import tacos.web.api.dto.OrderCreateRequest;
import tacos.web.api.dto.OrderResponse;
import tacos.web.api.mapper.IngredientMapper;
import tacos.web.api.mapper.OrderMapper;
import tacos.service.TacoClassificationService;

import java.util.Date;
import tacos.PaymentMethod;

public class DtoMapperContractTest {

  private ObjectMapper objectMapper;
  private IngredientMapper ingredientMapper;
  private OrderMapper orderMapper;

  @BeforeEach
  public void setUp() {
    objectMapper =
        new ObjectMapper()
            .findAndRegisterModules();

    ingredientMapper =
        new IngredientMapper();

    orderMapper =
        new OrderMapper(ingredientMapper, new TacoClassificationService());
  }

  @Test
  public void shouldNotSerializeSensitiveOrderFields()
      throws Exception {

    User owner = testUser();

    TacoOrder order = new TacoOrder();
    order.setId("order-1");
    order.setUser(owner);
    order.setPaymentMethodId("pm-1");
    order.setPaymentBrand("VISA");
    order.setPaymentLast4("1111");

    Ingredient ingredient =
        new Ingredient(
            "FLTO",
            "Flour Tortilla",
            Type.WRAP);

    Taco taco = new Taco();
    taco.setId("taco-1");
    taco.setName("Test taco");
    taco.setIngredients(
        List.of(ingredient));

    order.setTacos(List.of(taco));

    OrderResponse response =
        orderMapper.toResponse(order);

    String serialized =
        objectMapper.writeValueAsString(response);

    JsonNode json =
        objectMapper.readTree(serialized);

    assertEquals(
        "order-1",
        json.get("id").asText());

    assertEquals(
        "owner-1",
        json.get("userId").asText());

    assertFalse(json.has("user"));
    assertFalse(json.has("ccNumber"));
    assertFalse(json.has("ccExpiration"));
    assertFalse(json.has("ccCVV"));
    assertFalse(json.has("password"));
    assertFalse(json.has("authorities"));

    assertFalse(
        serialized.contains(
            "4111111111111111"));

    assertFalse(
        serialized.contains("\"123\""));
  }

  @Test
  public void shouldIgnoreServerOwnedRequestFields()
      throws Exception {

    String json =
        "{"
            + "\"id\":\"attacker-order\","
            + "\"placedAt\":\"1970-01-01T00:00:00Z\","
            + "\"status\":\"DELIVERED\","
            + "\"total\":0,"
            + "\"userId\":\"another-user\","
            + "\"deliveryName\":\"Daniel\","
            + "\"deliveryStreet\":\"Main Street\","
            + "\"deliveryCity\":\"Guadalajara\","
            + "\"deliveryState\":\"Jalisco\","
            + "\"deliveryZip\":\"44100\","
            + "\"paymentMethodId\":\"pm-1\","
            + "\"items\":["
            + "{"
            + "\"taco\":{"
            + "\"id\":\"client-taco-id\","
            + "\"name\":\"Test taco\","
            + "\"ingredientIds\":[\"FLTO\"]"
            + "},"
            + "\"quantity\":2"
            + "}"
            + "]"
            + "}";

    OrderCreateRequest request =
        objectMapper.readValue(
            json,
            OrderCreateRequest.class);

    User authenticatedUser = testUser();

    Ingredient realIngredient =
        new Ingredient(
            "FLTO",
            "Flour Tortilla",
            Type.WRAP);

    Taco resolvedTaco =
        orderMapper.toTaco(
            request.getItems().get(0).getTaco(),
            List.of(realIngredient));

    PaymentMethod paymentMethod = new PaymentMethod("pm-1", "owner-1", "token-test",
    "VISA", "1111", 12, 2099, new Date());

    OrderItem item = new OrderItem(resolvedTaco, 2, new BigDecimal("0.75"), new BigDecimal("1.50"));
    TacoOrder entity = orderMapper.toEntity(request, authenticatedUser, paymentMethod, List.of(item), "MXN");

    assertNull(entity.getId());

    assertSame(
        authenticatedUser,
        entity.getUser());

    assertEquals(
        OrderStatus.PLACED,
        entity.getStatus());

    assertEquals(new BigDecimal("1.50"), entity.getTotal());

    assertEquals(
        "Flour Tortilla",
        entity.getTacos()
            .get(0)
            .getIngredients()
            .get(0)
            .getName());

    assertEquals(
        Type.WRAP,
        entity.getTacos()
            .get(0)
            .getIngredients()
            .get(0)
            .getType());
  }

  @Test
  public void shouldMapIngredientRequestAndResponse() {
    IngredientRequest request =
        new IngredientRequest();

    request.setName("Flour Tortilla");
    request.setType(Type.WRAP);

    Ingredient entity =
        ingredientMapper.toEntity(request);

    assertNull(entity.getId());
    assertEquals(
        "Flour Tortilla",
        entity.getName());

    entity.setId("FLTO");

    IngredientResponse response =
        ingredientMapper.toResponse(entity);

    assertEquals("FLTO", response.getId());
    assertEquals(
        "Flour Tortilla",
        response.getName());
    assertEquals(Type.WRAP, response.getType());
  }

  private User testUser() {
    User user = new User(
        "daniel",
        "{noop}secret-password",
        "Daniel",
        "Main Street",
        "Guadalajara",
        "Jalisco",
        "44100",
        "3312345678",
        "daniel@example.com");

    user.setId("owner-1");

    return user;
  }
}
