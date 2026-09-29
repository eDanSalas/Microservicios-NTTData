package tacos.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;
import tacos.Ingredient;
import tacos.OrderItem;
import tacos.TacoOrder;
import tacos.User;
import tacos.data.OrderRepository;
import tacos.web.api.dto.OrderCreateRequest;
import tacos.web.api.dto.OrderItemRequest;
import tacos.web.api.dto.OrderTacoRequest;
import tacos.web.api.dto.ReorderRequest;

@Service
public class ReorderService {
  private final OrderHistoryService historyService;
  private final OrderCreationService creationService;
  private final OrderRepository orderRepo;

  public ReorderService(OrderHistoryService historyService, OrderCreationService creationService,
      OrderRepository orderRepo) {
    this.historyService = historyService;
    this.creationService = creationService;
    this.orderRepo = orderRepo;
  }

  public Mono<ReorderResult> reorder(User user, String sourceOrderId, ReorderRequest request,
      String idempotencyKey) {
    String userId = userId(user);
    if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 100)
      return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Idempotency-Key must contain between 1 and 100 characters"));
    String orderId = UUID.nameUUIDFromBytes(
        (userId + ":" + sourceOrderId + ":" + idempotencyKey).getBytes(StandardCharsets.UTF_8))
        .toString();
    return historyService.findMine(user, sourceOrderId).flatMap(source ->
        orderRepo.findByIdAndUserId(orderId, userId).map(order -> created(source, order))
            .switchIfEmpty(Mono.defer(() -> execute(user, source, request, orderId))));
  }

  private Mono<ReorderResult> execute(User user, TacoOrder source, ReorderRequest request,
      String orderId) {
    OrderCreateRequest createRequest = request(source, request.getPaymentMethodId());
    return creationService.quote(createRequest, user).flatMap(current -> {
      List<ReorderDifference> differences = differences(source, current);
      if (!differences.isEmpty() && !request.isConfirmPriceChange())
        return Mono.just(quote(source, current, differences));
      return creationService.create(createRequest, user, orderId)
          .map(order -> created(source, order, differences));
    });
  }

  private OrderCreateRequest request(TacoOrder source, String paymentMethodId) {
    if (source.getItems() == null || source.getItems().isEmpty()) throw new ResponseStatusException(
        HttpStatus.UNPROCESSABLE_ENTITY, "Source order has no items");
    OrderCreateRequest request = new OrderCreateRequest();
    request.setDeliveryName(source.getDeliveryName());
    request.setDeliveryStreet(source.getDeliveryStreet());
    request.setDeliveryCity(source.getDeliveryCity());
    request.setDeliveryState(source.getDeliveryState());
    request.setDeliveryZip(source.getDeliveryZip());
    request.setPaymentMethodId(paymentMethodId);
    request.setCouponCode(source.getCouponCode());
    request.setItems(source.getItems().stream().map(this::item).toList());
    return request;
  }

  private OrderItemRequest item(OrderItem source) {
    if (source.getTaco() == null || source.getTaco().getIngredients() == null)
      throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
          "Source order contains an invalid taco");
    OrderTacoRequest taco = new OrderTacoRequest();
    taco.setName(source.getTaco().getName());
    taco.setIngredientIds(source.getTaco().getIngredients().stream().map(Ingredient::getId).toList());
    OrderItemRequest item = new OrderItemRequest();
    item.setTaco(taco);
    item.setQuantity(source.getQuantity());
    return item;
  }

  private List<ReorderDifference> differences(TacoOrder source, TacoOrder current) {
    List<ReorderDifference> result = new ArrayList<>();
    int size = Math.min(source.getItems().size(), current.getItems().size());
    for (int index = 0; index < size; index++) {
      OrderItem previous = source.getItems().get(index);
      OrderItem updated = current.getItems().get(index);
      String tacoName = previous.getTaco().getName();
      if (different(previous.getUnitPriceAtPurchase(), updated.getUnitPriceAtPurchase()))
        result.add(new ReorderDifference("UNIT_PRICE", tacoName,
            value(previous.getUnitPriceAtPurchase()), value(updated.getUnitPriceAtPurchase())));
      String previousIngredients = ingredients(previous);
      String currentIngredients = ingredients(updated);
      if (!previousIngredients.equals(currentIngredients))
        result.add(new ReorderDifference("INGREDIENTS", tacoName, previousIngredients,
            currentIngredients));
    }
    if (different(source.getTotal(), current.getTotal()))
      result.add(new ReorderDifference("TOTAL", null, value(source.getTotal()),
          value(current.getTotal())));
    return result;
  }

  private String ingredients(OrderItem item) {
    return item.getTaco().getIngredients().stream()
        .map(ingredient -> ingredient.getId() + ":" + ingredient.getName())
        .collect(Collectors.joining(","));
  }

  private boolean different(BigDecimal first, BigDecimal second) {
    return first == null ? second != null : second == null || first.compareTo(second) != 0;
  }

  private String value(BigDecimal value) {
    return value == null ? null : value.toPlainString();
  }

  private ReorderResult quote(TacoOrder source, TacoOrder current,
      List<ReorderDifference> differences) {
    return new ReorderResult("QUOTE", source.getId(), null, source.getTotal(), current.getTotal(),
        current.getCurrency(), true, differences);
  }

  private ReorderResult created(TacoOrder source, TacoOrder order) {
    return created(source, order, differences(source, order));
  }

  private ReorderResult created(TacoOrder source, TacoOrder order,
      List<ReorderDifference> differences) {
    return new ReorderResult("CREATED", source.getId(), order, source.getTotal(), order.getTotal(),
        order.getCurrency(), false, differences);
  }

  private String userId(User user) {
    if (user == null || user.getId() == null || user.getId().isBlank()) throw new ResponseStatusException(
        HttpStatus.UNAUTHORIZED, "Authentication is required");
    return user.getId();
  }
}
