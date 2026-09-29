package tacos.web.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;
import tacos.User;
import tacos.service.OrderHistoryService;
import tacos.web.api.dto.OrderDetailResponse;
import tacos.web.api.dto.OrderHistoryPageResponse;
import tacos.web.api.dto.OrderSummaryResponse;
import tacos.web.api.mapper.OrderHistoryMapper;

@RestController
@RequestMapping(path = "/api/users/me/orders", produces = "application/json")
public class UserOrderHistoryController {
  private final OrderHistoryService service;
  private final OrderHistoryMapper mapper;

  public UserOrderHistoryController(OrderHistoryService service, OrderHistoryMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  @GetMapping
  public Mono<OrderHistoryPageResponse<OrderSummaryResponse>> findAll(
      @AuthenticationPrincipal User user, @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return service.findMine(user, page, size).map(result -> new OrderHistoryPageResponse<>(
        result.getContent().stream().map(mapper::toSummary).toList(), result.getPage(),
        result.getSize(), result.getTotalElements(), result.getTotalPages()));
  }

  @GetMapping("/{id}")
  public Mono<OrderDetailResponse> findOne(@AuthenticationPrincipal User user,
      @PathVariable String id) {
    return service.findMine(user, id).map(mapper::toDetail);
  }
}
