package tacos.web.api;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;
import tacos.User;
import tacos.service.ReorderResult;
import tacos.service.ReorderService;
import tacos.web.api.dto.ReorderDifferenceResponse;
import tacos.web.api.dto.ReorderRequest;
import tacos.web.api.dto.ReorderResponse;
import tacos.web.api.mapper.OrderMapper;

@RestController
@RequestMapping(path = "/api/orders", produces = "application/json")
public class ReorderController {
  private final ReorderService service;
  private final OrderMapper mapper;

  public ReorderController(ReorderService service, OrderMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  @PostMapping(path = "/{id}/reorder", consumes = "application/json")
  public Mono<ResponseEntity<ReorderResponse>> reorder(@PathVariable String id,
      @Valid @RequestBody ReorderRequest request,
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @AuthenticationPrincipal User user) {
    return service.reorder(user, id, request, idempotencyKey).map(result -> ResponseEntity
        .status("CREATED".equals(result.getStatus()) ? HttpStatus.CREATED : HttpStatus.OK)
        .body(response(result)));
  }

  private ReorderResponse response(ReorderResult result) {
    return new ReorderResponse(result.getStatus(), result.getSourceOrderId(),
        result.getOrder() == null ? null : mapper.toResponse(result.getOrder()),
        result.getPreviousTotal(), result.getCurrentTotal(), result.getCurrency(),
        result.isConfirmationRequired(), result.getDifferences().stream()
            .map(difference -> new ReorderDifferenceResponse(difference.getType(),
                difference.getTacoName(), difference.getPreviousValue(),
                difference.getCurrentValue())).toList());
  }
}
