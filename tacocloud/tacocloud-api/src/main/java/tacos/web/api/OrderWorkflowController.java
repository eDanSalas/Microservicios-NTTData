package tacos.web.api;

import javax.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;
import tacos.User;
import tacos.service.OrderWorkflowService;
import tacos.web.api.dto.OrderCancellationRequest;
import tacos.web.api.dto.OrderDetailResponse;
import tacos.web.api.dto.OrderStatusUpdateRequest;
import tacos.web.api.mapper.OrderHistoryMapper;

@RestController
@RequestMapping(path = "/api/orders", produces = "application/json")
public class OrderWorkflowController {
  private final OrderWorkflowService service;
  private final OrderHistoryMapper mapper;

  public OrderWorkflowController(OrderWorkflowService service, OrderHistoryMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  @PatchMapping(path = "/{id}/status", consumes = "application/json")
  public Mono<OrderDetailResponse> transition(@PathVariable String id,
      @Valid @RequestBody OrderStatusUpdateRequest request,
      @AuthenticationPrincipal User user) {
    return service.transition(id, request.getStatus(), request.getReason(), user)
        .map(mapper::toDetail);
  }

  @PostMapping(path = "/{id}/cancel", consumes = "application/json")
  public Mono<OrderDetailResponse> cancel(@PathVariable String id,
      @Valid @RequestBody(required = false) OrderCancellationRequest request,
      @AuthenticationPrincipal User user) {
    return service.cancel(id, request == null ? null : request.getReason(), user)
        .map(mapper::toDetail);
  }
}
