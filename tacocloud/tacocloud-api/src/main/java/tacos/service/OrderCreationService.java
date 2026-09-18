package tacos.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.OrderItem;
import tacos.Taco;
import tacos.TacoOrder;
import tacos.User;
import tacos.data.OrderRepository;
import tacos.data.PaymentMethodRepository;
import tacos.inventory.InventoryService;
import tacos.messaging.OrderMessagingService;
import tacos.pricing.CouponService;
import tacos.web.api.dto.OrderCreateRequest;
import tacos.web.api.dto.OrderItemRequest;
import tacos.web.api.dto.OrderTacoRequest;
import tacos.web.api.mapper.OrderMapper;

@Service
public class OrderCreationService {

  private final OrderRepository orderRepo;
  private final OrderMessagingService orderMessages;
  private final OrderMapper orderMapper;
  private final PaymentMethodRepository paymentMethodRepo;
  private final OrderPricingService pricingService;
  private final CouponService couponService;
  private final InventoryService inventoryService;
  private final TacoDesignService designService;


  public OrderCreationService(OrderRepository orderRepo, OrderMessagingService orderMessages,
      OrderMapper orderMapper,
      PaymentMethodRepository paymentMethodRepo,
      OrderPricingService pricingService,
      CouponService couponService,
      InventoryService inventoryService,
      TacoDesignService designService) {

    this.orderRepo = orderRepo;
    this.orderMessages = orderMessages;
    this.orderMapper = orderMapper;
    this.paymentMethodRepo = paymentMethodRepo;
    this.pricingService = pricingService;
    this.couponService = couponService;
    this.inventoryService = inventoryService;
    this.designService = designService;
  }

  public Mono<TacoOrder> create(OrderCreateRequest request, User authenticatedUser) {
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
                  return order;
                }))
        .flatMap(order -> {
          order.setId(UUID.randomUUID().toString());
          return inventoryService.reserve(order, order.getId()).then(orderRepo.save(order))
              .onErrorResume(error -> inventoryService.release(order.getId()).then(Mono.error(error)))
              .flatMap(savedOrder -> publish(savedOrder).thenReturn(savedOrder));
        });
    }

  private Mono<OrderItem> resolveItem(OrderItemRequest request) {
    return resolveTaco(request.getTaco()).map(taco -> pricingService.price(taco, request.getQuantity()));
  }

  private Mono<Taco> resolveTaco(OrderTacoRequest request) {
    return designService.resolveAndRequireValid(request.getName(), request.getIngredientIds());
  }

  private Mono<Void> publish(TacoOrder savedOrder) {
    return Mono.fromRunnable(() -> orderMessages.sendOrder(savedOrder));
  }
}
