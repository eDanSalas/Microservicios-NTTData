package tacos.web.api.mapper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import tacos.Ingredient;
import tacos.OrderItem;
import tacos.PaymentMethod;
import tacos.Taco;
import tacos.TacoOrder;
import tacos.User;
import tacos.service.TacoClassificationService;
import tacos.web.api.dto.OrderCreateRequest;
import tacos.web.api.dto.OrderItemResponse;
import tacos.web.api.dto.OrderResponse;
import tacos.web.api.dto.OrderTacoRequest;
import tacos.web.api.dto.OrderTacoResponse;
import tacos.web.api.dto.PaymentMethodSummaryResponse;

@Component
public class OrderMapper {

  private final IngredientMapper ingredientMapper;
  private final TacoClassificationService classificationService;

  public OrderMapper(
      IngredientMapper ingredientMapper,
      TacoClassificationService classificationService) {

    this.ingredientMapper = ingredientMapper;
    this.classificationService = classificationService;
  }

  public Taco toTaco(
      OrderTacoRequest request,
      List<Ingredient> ingredients) {

    Taco taco = new Taco();

    taco.setName(request.getName());
    taco.setIngredients(ingredients);

    return taco;
  }

  public TacoOrder toEntity(OrderCreateRequest request, User authenticatedUser, PaymentMethod paymentMethod,
      List<OrderItem> items, String currency) {
        TacoOrder order = new TacoOrder();

        order.setUser(authenticatedUser);

        order.setDeliveryName(
            request.getDeliveryName());
        order.setDeliveryStreet(
            request.getDeliveryStreet());
        order.setDeliveryCity(
            request.getDeliveryCity());
        order.setDeliveryState(
            request.getDeliveryState());
        order.setDeliveryZip(
            request.getDeliveryZip());

        order.setPaymentMethodId(
            paymentMethod.getId());
        order.setPaymentBrand(
            paymentMethod.getBrand());
        order.setPaymentLast4(
            paymentMethod.getLast4());

        order.setItems(items);
        order.setSubtotal(items.stream().map(OrderItem::getSubtotal).reduce(java.math.BigDecimal.ZERO,
            java.math.BigDecimal::add).setScale(2));
        order.setDiscount(java.math.BigDecimal.ZERO.setScale(2));
        order.setTotal(order.getSubtotal());
        order.setCurrency(currency);

        return order;
    }

  public OrderResponse toResponse(TacoOrder order) {
    String userId = order.getUser() != null ? order.getUser().getId() : null;

    PaymentMethodSummaryResponse payment = order.getPaymentBrand() == null ? null : new PaymentMethodSummaryResponse(
            order.getPaymentBrand(),
            order.getPaymentLast4());

    return new OrderResponse(
        order.getId(),
        order.getPlacedAt(),
        order.getStatus(),
        userId,
        order.getDeliveryName(),
        order.getDeliveryStreet(),
        order.getDeliveryCity(),
        order.getDeliveryState(),
        order.getDeliveryZip(),
        payment,
        order.getSubtotal(),
        order.getDiscount(),
        order.getTotal(),
        order.getCurrency(),
        order.getCouponCode(),
        mapItems(order.getItems()));
  }

  private List<OrderItemResponse> mapItems(List<OrderItem> items) {
    if (items == null) return Collections.emptyList();
    return items.stream().map(item -> new OrderItemResponse(toTacoResponse(item.getTaco()),
        item.getQuantity(), item.getUnitPriceAtPurchase(), item.getSubtotal())).collect(Collectors.toList());
  }

  private List<OrderTacoResponse> mapTacos(
      List<Taco> tacos) {

    if (tacos == null) {
      return Collections.emptyList();
    }

    return tacos.stream()
        .map(this::toTacoResponse)
        .collect(Collectors.toList());
  }

  private OrderTacoResponse toTacoResponse(
      Taco taco) {

    List<Ingredient> ingredients =
        taco.getIngredients() != null
            ? taco.getIngredients()
            : Collections.emptyList();

    return new OrderTacoResponse(
        taco.getId(),
        taco.getName(),
        taco.getCreatedAt(),
        ingredients.stream()
            .map(ingredientMapper::toResponse)
            .collect(Collectors.toList()),
        classificationService.classify(taco));
  }
}
