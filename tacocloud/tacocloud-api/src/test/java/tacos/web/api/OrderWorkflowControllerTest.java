package tacos.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.OrderStatus;
import tacos.TacoOrder;
import tacos.User;
import tacos.service.OrderWorkflowService;
import tacos.web.api.dto.OrderCancellationRequest;
import tacos.web.api.dto.OrderDetailResponse;
import tacos.web.api.dto.OrderStatusUpdateRequest;
import tacos.web.api.mapper.OrderHistoryMapper;

public class OrderWorkflowControllerTest {
  @Test
  public void shouldDelegateStatusTransition() {
    OrderWorkflowService service = mock(OrderWorkflowService.class);
    OrderHistoryMapper mapper = mock(OrderHistoryMapper.class);
    User user = mock(User.class);
    TacoOrder order = new TacoOrder();
    OrderStatusUpdateRequest request = new OrderStatusUpdateRequest();
    request.setStatus(OrderStatus.ACCEPTED);
    request.setReason("Accepted");
    OrderDetailResponse response = response(OrderStatus.ACCEPTED);
    when(service.transition("order-1", OrderStatus.ACCEPTED, "Accepted", user))
        .thenReturn(Mono.just(order));
    when(mapper.toDetail(order)).thenReturn(response);

    StepVerifier.create(new OrderWorkflowController(service, mapper)
        .transition("order-1", request, user))
        .assertNext(result -> assertEquals(OrderStatus.ACCEPTED, result.getStatus()))
        .verifyComplete();
  }

  @Test
  public void shouldDelegateOwnerCancellation() {
    OrderWorkflowService service = mock(OrderWorkflowService.class);
    OrderHistoryMapper mapper = mock(OrderHistoryMapper.class);
    User user = mock(User.class);
    TacoOrder order = new TacoOrder();
    OrderCancellationRequest request = new OrderCancellationRequest();
    request.setReason("Changed plans");
    when(service.cancel("order-1", "Changed plans", user)).thenReturn(Mono.just(order));
    when(mapper.toDetail(order)).thenReturn(response(OrderStatus.CANCELLED));

    StepVerifier.create(new OrderWorkflowController(service, mapper).cancel("order-1", request, user))
        .assertNext(result -> assertEquals(OrderStatus.CANCELLED, result.getStatus()))
        .verifyComplete();
  }

  private OrderDetailResponse response(OrderStatus status) {
    return new OrderDetailResponse("order-1", null, status, null, null, null, null, null,
        null, null, null, null, "MXN", List.of(), List.of());
  }
}
