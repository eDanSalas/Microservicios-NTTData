package tacos.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;
import tacos.TacoOrder;
import tacos.inventory.InventoryService;
import tacos.outbox.OrderOutboxService;
import tacos.web.api.EmailOrder;
import tacos.web.api.EmailOrderService;

@Service
public class EmailOrderSubmissionService {

  private final EmailOrderService emailOrderService;
  private final OrderOutboxService orderOutbox;
  private final InventoryService inventoryService;

  public EmailOrderSubmissionService(
      EmailOrderService emailOrderService,
      OrderOutboxService orderOutbox,
      InventoryService inventoryService) {

    this.emailOrderService = emailOrderService;
    this.orderOutbox = orderOutbox;
    this.inventoryService = inventoryService;
  }

  public Mono<TacoOrder> submit(
      Mono<EmailOrder> emailOrder) {

    return emailOrderService
        .convertEmailOrderToDomainOrder(emailOrder)
        .flatMap(order -> {
          order.setId(UUID.randomUUID().toString());
          return inventoryService.reserve(order, order.getId()).then(orderOutbox.save(order))
              .onErrorResume(error -> inventoryService.release(order.getId()).then(Mono.error(error)));
        });
  }
}
