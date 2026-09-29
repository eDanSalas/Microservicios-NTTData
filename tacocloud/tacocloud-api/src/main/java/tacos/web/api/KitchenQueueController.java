package tacos.web.api;

import java.util.List;

import javax.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.User;
import tacos.service.KitchenOrderView;
import tacos.service.KitchenQueueService;
import tacos.web.api.dto.KitchenOrderLineResponse;
import tacos.web.api.dto.KitchenOrderResponse;
import tacos.web.api.dto.OrderStatusUpdateRequest;

@RestController
@RequestMapping(path = "/api/kitchen", produces = "application/json")
public class KitchenQueueController {
  private final KitchenQueueService service;

  public KitchenQueueController(KitchenQueueService service) {
    this.service = service;
  }

  @GetMapping("/queue")
  public Flux<KitchenOrderResponse> queue(@AuthenticationPrincipal User cook) {
    return service.queue(cook).map(this::response);
  }

  @PostMapping("/orders/claim")
  public Mono<KitchenOrderResponse> claim(@AuthenticationPrincipal User cook) {
    return service.claimNext(cook).map(this::response);
  }

  @PatchMapping(path = "/orders/{id}/status", consumes = "application/json")
  public Mono<KitchenOrderResponse> updateStatus(@PathVariable String id,
      @Valid @RequestBody OrderStatusUpdateRequest request,
      @AuthenticationPrincipal User cook) {
    return service.updateStatus(id, request.getStatus(), request.getReason(), cook)
        .map(this::response);
  }

  private KitchenOrderResponse response(KitchenOrderView view) {
    List<KitchenOrderLineResponse> items = view.getOrder().getItems() == null ? List.of()
        : view.getOrder().getItems().stream().map(item -> new KitchenOrderLineResponse(
            item.getTaco() == null ? null : item.getTaco().getName(), item.getQuantity())).toList();
    return new KitchenOrderResponse(view.getOrder().getId(), view.getOrder().getPlacedAt(),
        view.getOrder().getStatus(), view.getOrder().getStationId(), view.getOrder().getCookId(),
        view.getOrder().getAcceptedAt(), view.getEstimatedPrepMinutes(), items);
  }
}
