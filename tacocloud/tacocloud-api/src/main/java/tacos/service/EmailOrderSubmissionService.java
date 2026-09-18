package tacos.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;
import tacos.TacoOrder;
import tacos.data.OrderRepository;
import tacos.messaging.OrderMessagingService;
import tacos.inventory.InventoryService;
import tacos.web.api.EmailOrder;
import tacos.web.api.EmailOrderService;

@Service
public class EmailOrderSubmissionService {

  private final EmailOrderService emailOrderService;
  private final OrderRepository orderRepo;
  private final OrderMessagingService orderMessages;
  private final InventoryService inventoryService;

  public EmailOrderSubmissionService(
      EmailOrderService emailOrderService,
      OrderRepository orderRepo,
      OrderMessagingService orderMessages,
      InventoryService inventoryService) {

    this.emailOrderService = emailOrderService;
    this.orderRepo = orderRepo;
    this.orderMessages = orderMessages;
    this.inventoryService = inventoryService;
  }

  public Mono<TacoOrder> submit(
      Mono<EmailOrder> emailOrder) {

    return emailOrderService
        .convertEmailOrderToDomainOrder(emailOrder)
        .flatMap(order -> {
          order.setId(UUID.randomUUID().toString());
          return inventoryService.reserve(order, order.getId()).then(orderRepo.save(order))
              .onErrorResume(error -> inventoryService.release(order.getId()).then(Mono.error(error)))
              .flatMap(savedOrder -> publish(savedOrder).thenReturn(savedOrder));
        });
  }

  private Mono<Void> publish(
      TacoOrder savedOrder) {

    /*
     * Adaptador temporal porque OrderMessagingService
     * todavía devuelve void.
     *
     * TC-29 reemplazará esta coordinación por outbox.
     */
    return Mono.fromRunnable(
        () -> orderMessages.sendOrder(savedOrder));
  }
}
