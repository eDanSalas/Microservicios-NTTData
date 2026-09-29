package tacos.service;

import java.util.List;
import java.util.UUID;
import java.time.DateTimeException;
import java.time.YearMonth;
import java.util.Date;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.OrderItem;
import tacos.OrderStatusChange;
import tacos.PaymentMethod;
import tacos.Taco;
import tacos.TacoOrder;
import tacos.User;
import tacos.data.PaymentMethodRepository;
import tacos.inventory.InventoryService;
import tacos.observability.BusinessMetrics;
import tacos.outbox.OrderOutboxService;
import tacos.pricing.CouponService;
import tacos.web.api.dto.OrderCreateRequest;
import tacos.web.api.dto.OrderItemRequest;
import tacos.web.api.dto.OrderTacoRequest;
import tacos.web.api.mapper.OrderMapper;

@Service
public class OrderCreationService {

  private final OrderOutboxService orderOutbox;
  private final OrderMapper orderMapper;
  private final PaymentMethodRepository paymentMethodRepo;
  private final OrderPricingService pricingService;
  private final CouponService couponService;
  private final InventoryService inventoryService;
  private final TacoDesignService designService;
  private final BusinessMetrics metrics;


  public OrderCreationService(OrderOutboxService orderOutbox, OrderMapper orderMapper,
      PaymentMethodRepository paymentMethodRepo,
      OrderPricingService pricingService,
      CouponService couponService,
      InventoryService inventoryService,
      TacoDesignService designService, BusinessMetrics metrics) {

    this.orderOutbox = orderOutbox;
    this.orderMapper = orderMapper;
    this.paymentMethodRepo = paymentMethodRepo;
    this.pricingService = pricingService;
    this.couponService = couponService;
    this.inventoryService = inventoryService;
    this.designService = designService;
    this.metrics = metrics;
  }

  public Mono<TacoOrder> create(OrderCreateRequest request, User authenticatedUser) {
    return create(request, authenticatedUser, UUID.randomUUID().toString());
  }

  public Mono<TacoOrder> create(OrderCreateRequest request, User authenticatedUser, String orderId) {
    if (orderId == null || orderId.isBlank()) return Mono.error(new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "Order id is required"));
    Mono<TacoOrder> placement = quote(request, authenticatedUser).flatMap(order -> {
      order.setId(orderId);
      return inventoryService.reserve(order, orderId).then(orderOutbox.save(order))
          .onErrorResume(error -> inventoryService.release(orderId).then(Mono.error(error)));
    });
    return metrics.placement(placement, "api",
        request.getCouponCode() != null && !request.getCouponCode().isBlank());
  }

  public Mono<TacoOrder> quote(OrderCreateRequest request, User authenticatedUser) {
    String userId = authenticatedUser != null
            ? authenticatedUser.getId()
            : null;

    if (userId == null) {
        return Mono.error(
            new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Authentication is required"));
    }

    return paymentMethodRepo
        .findByIdAndUserId(request.getPaymentMethodId(), userId)
        .filter(this::isValid)
        .switchIfEmpty(
            Mono.error(
                new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Payment method is not available")))
        .flatMap(paymentMethod ->
            Flux.fromIterable(request.getItems())
                .concatMap(this::resolveItem)
                .collectList()
                .map(items -> {
                  TacoOrder order = orderMapper.toEntity(
                        request,
                        authenticatedUser,
                        paymentMethod,
                        items,
                        pricingService.getCurrency());
                  couponService.apply(order, request.getCouponCode());
                  order.getStatusHistory().add(new OrderStatusChange(null, order.getStatus(),
                      new Date(order.getPlacedAt().getTime()), authenticatedUser.getId(),
                      role(authenticatedUser),
                      "API_CREATE", "ORDER_CREATED"));
                  return order;
                }));
  }

  private boolean isValid(PaymentMethod paymentMethod) {
    try {
      YearMonth expiration = YearMonth.of(paymentMethod.getExpirationYear(),
          paymentMethod.getExpirationMonth());
      return !expiration.isBefore(YearMonth.now());
    } catch (DateTimeException exception) {
      return false;
    }
  }

  private Mono<OrderItem> resolveItem(OrderItemRequest request) {
    return resolveTaco(request.getTaco()).map(taco -> pricingService.price(taco, request.getQuantity()));
  }

  private Mono<Taco> resolveTaco(OrderTacoRequest request) {
    return designService.resolveAndRequireValid(request.getName(), request.getIngredientIds());
  }

  private String role(User user) {
    return user.getAuthorities() != null && user.getAuthorities().stream()
        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())) ? "ADMIN" : "USER";
  }
}
