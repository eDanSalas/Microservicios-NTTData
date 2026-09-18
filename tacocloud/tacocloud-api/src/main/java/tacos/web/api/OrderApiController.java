package tacos.web.api;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.User;
import tacos.data.OrderRepository;
import tacos.service.EmailOrderSubmissionService;
import tacos.service.OrderCreationService;
import tacos.service.OrderService;
import tacos.web.api.dto.OrderCreateRequest;
import tacos.web.api.dto.OrderResponse;
import tacos.web.api.mapper.OrderMapper;

@RestController
@RequestMapping(
    path = "/api/orders",
    produces = "application/json")
@CrossOrigin(origins = "http://localhost:8080")
public class OrderApiController {

  private final OrderRepository repo;
  private final EmailOrderSubmissionService
      emailOrderSubmissionService;
  private final OrderService orderService;
  private final OrderCreationService
      orderCreationService;
  private final OrderMapper orderMapper;

  public OrderApiController(
      OrderRepository repo,
      EmailOrderSubmissionService
          emailOrderSubmissionService,
      OrderService orderService,
      OrderCreationService orderCreationService,
      OrderMapper orderMapper) {

    this.repo = repo;
    this.emailOrderSubmissionService =
        emailOrderSubmissionService;
    this.orderService = orderService;
    this.orderCreationService =
        orderCreationService;
    this.orderMapper = orderMapper;
  }

  @GetMapping
    public Flux<OrderResponse> allOrders(@AuthenticationPrincipal User authenticatedUser) {
        return orderService
            .findVisibleOrders(authenticatedUser)
            .map(orderMapper::toResponse);
    }

  @PostMapping(consumes = "application/json")
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<OrderResponse> postOrder(
      @Valid @RequestBody
          OrderCreateRequest request,
      @AuthenticationPrincipal
          User authenticatedUser) {

    return orderCreationService
        .create(request, authenticatedUser)
        .map(orderMapper::toResponse);
  }

  @PostMapping(
      path = "fromEmail",
      consumes = "application/json")
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<OrderResponse> postOrderFromEmail(
      @Valid @RequestBody Mono<EmailOrder> emailOrder) {

    return emailOrderSubmissionService
        .submit(emailOrder)
        .map(orderMapper::toResponse);
  }

  @PatchMapping(
      path = "/{orderId}",
      consumes = "application/json")
  public Mono<OrderResponse> patchOrder(
      @PathVariable("orderId") String orderId,
      @Valid @RequestBody
          OrderPatchRequest patch,
      @AuthenticationPrincipal
          User authenticatedUser) {

    return orderService
        .patchOrder(
            orderId,
            patch,
            authenticatedUser)
        .map(orderMapper::toResponse);
  }

  @PutMapping(
      path = "/{orderId}",
      consumes = "application/json")
  public Mono<OrderResponse> putOrder(
      @PathVariable("orderId") String orderId,
      @Valid @RequestBody
          OrderReplaceRequest request,
      @AuthenticationPrincipal
          User authenticatedUser) {

    return orderService
        .replaceOrder(
            orderId,
            request,
            authenticatedUser)
        .map(orderMapper::toResponse);
  }

  @DeleteMapping("/{orderId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> deleteOrder(
      @PathVariable("orderId") String orderId,
      @AuthenticationPrincipal
          User authenticatedUser) {

    return orderService.deleteOrder(
        orderId,
        authenticatedUser);
  }
}