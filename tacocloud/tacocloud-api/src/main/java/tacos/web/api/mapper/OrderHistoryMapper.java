package tacos.web.api.mapper;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import tacos.OrderItem;
import tacos.OrderStatusChange;
import tacos.TacoOrder;
import tacos.web.api.dto.AdminOrderSummaryResponse;
import tacos.web.api.dto.OrderDetailResponse;
import tacos.web.api.dto.OrderResponse;
import tacos.web.api.dto.OrderStatusChangeResponse;
import tacos.web.api.dto.OrderSummaryResponse;

@Component
public class OrderHistoryMapper {
  private final OrderMapper orderMapper;

  public OrderHistoryMapper(OrderMapper orderMapper) {
    this.orderMapper = orderMapper;
  }

  public OrderSummaryResponse toSummary(TacoOrder order) {
    return new OrderSummaryResponse(order.getId(), order.getPlacedAt(), order.getStatus(),
        order.getTotal(), order.getCurrency(), itemCount(order));
  }

  public AdminOrderSummaryResponse toAdminSummary(TacoOrder order) {
    return new AdminOrderSummaryResponse(order.getId(), order.getPlacedAt(), order.getStatus(),
        order.getTotal(), order.getCurrency(), itemCount(order),
        order.getUserId() != null ? order.getUserId()
            : order.getUser() == null ? null : order.getUser().getId());
  }

  public OrderDetailResponse toDetail(TacoOrder order) {
    OrderResponse response = orderMapper.toResponse(order);
    return new OrderDetailResponse(response.getId(), response.getPlacedAt(), response.getStatus(),
        response.getDeliveryName(), response.getDeliveryStreet(), response.getDeliveryCity(),
        response.getDeliveryState(), response.getDeliveryZip(), response.getPaymentMethod(),
        response.getSubtotal(), response.getDiscount(), response.getTotal(), response.getCurrency(),
        response.getItems(), history(order));
  }

  private List<OrderStatusChangeResponse> history(TacoOrder order) {
    if (order.getStatusHistory() == null) return List.of();
    return order.getStatusHistory().stream().sorted(Comparator.comparing(OrderStatusChange::getChangedAt))
        .map(change -> new OrderStatusChangeResponse(change.getFromStatus(), change.getToStatus(),
            change.getChangedAt(), change.getActorId(), change.getActorRole(), change.getOrigin(),
            change.getReason())).toList();
  }

  private int itemCount(TacoOrder order) {
    return order.getItems() == null ? 0 : order.getItems().stream().mapToInt(OrderItem::getQuantity).sum();
  }
}
