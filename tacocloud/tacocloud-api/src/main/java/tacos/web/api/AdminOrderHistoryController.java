package tacos.web.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;
import tacos.OrderStatus;
import tacos.service.OrderHistoryService;
import tacos.web.api.dto.AdminOrderSummaryResponse;
import tacos.web.api.dto.OrderHistoryPageResponse;
import tacos.web.api.mapper.OrderHistoryMapper;

@RestController
@RequestMapping(path = "/api/admin/orders", produces = "application/json")
public class AdminOrderHistoryController {
  private final OrderHistoryService service;
  private final OrderHistoryMapper mapper;

  public AdminOrderHistoryController(OrderHistoryService service, OrderHistoryMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  @GetMapping
  public Mono<OrderHistoryPageResponse<AdminOrderSummaryResponse>> findAll(
      @RequestParam(required = false) String userId,
      @RequestParam(required = false) OrderStatus status,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return service.findAll(userId, status, page, size).map(result -> new OrderHistoryPageResponse<>(
        result.getContent().stream().map(mapper::toAdminSummary).toList(), result.getPage(),
        result.getSize(), result.getTotalElements(), result.getTotalPages()));
  }
}
