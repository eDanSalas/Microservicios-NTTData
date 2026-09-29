package tacos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.InventoryReservation;
import tacos.TacoOrder;
import tacos.inventory.InventoryService;
import tacos.outbox.OrderOutboxService;
import tacos.web.api.EmailOrder;
import tacos.web.api.EmailOrderService;

public class EmailOrderSubmissionServiceTest {

  private EmailOrderService emailOrderService;
  private OrderOutboxService orderOutbox;
  private EmailOrderSubmissionService service;
  private InventoryService inventoryService;

  @BeforeEach
  public void setUp() {
    emailOrderService = Mockito.mock(EmailOrderService.class);
    orderOutbox = Mockito.mock(OrderOutboxService.class);
    inventoryService = Mockito.mock(InventoryService.class);
    when(inventoryService.reserve(Mockito.any(TacoOrder.class), Mockito.anyString()))
        .thenReturn(Mono.just(new InventoryReservation()));
    when(inventoryService.release(Mockito.anyString())).thenReturn(Mono.empty());
    service = new EmailOrderSubmissionService(emailOrderService, orderOutbox, inventoryService);
  }

  @Test
  public void shouldConvertThenSaveWithOutbox() {
    Mono<EmailOrder> request = Mono.just(new EmailOrder());
    TacoOrder convertedOrder = new TacoOrder();
    TacoOrder savedOrder = new TacoOrder();
    savedOrder.setId("order-1");
    when(emailOrderService.convertEmailOrderToDomainOrder(request))
        .thenReturn(Mono.just(convertedOrder));
    when(orderOutbox.save(convertedOrder)).thenReturn(Mono.just(savedOrder));

    StepVerifier.create(service.submit(request)).assertNext(result -> assertSame(savedOrder, result))
        .verifyComplete();

    InOrder executionOrder = Mockito.inOrder(emailOrderService, orderOutbox);
    executionOrder.verify(emailOrderService).convertEmailOrderToDomainOrder(request);
    executionOrder.verify(orderOutbox).save(convertedOrder);
  }

  @Test
  public void shouldNotSaveWhenConversionFails() {
    Mono<EmailOrder> request = Mono.just(new EmailOrder());
    RuntimeException error = new RuntimeException("Conversion failed");
    when(emailOrderService.convertEmailOrderToDomainOrder(request)).thenReturn(Mono.error(error));

    StepVerifier.create(service.submit(request)).expectErrorSatisfies(result ->
        assertSame(error, result)).verify();

    verify(emailOrderService).convertEmailOrderToDomainOrder(request);
    verifyNoInteractions(orderOutbox);
  }

  @Test
  public void shouldReleaseReservationWhenTransactionalSaveFails() {
    Mono<EmailOrder> request = Mono.just(new EmailOrder());
    TacoOrder convertedOrder = new TacoOrder();
    RuntimeException error = new RuntimeException("Database failed");
    when(emailOrderService.convertEmailOrderToDomainOrder(request))
        .thenReturn(Mono.just(convertedOrder));
    when(orderOutbox.save(convertedOrder)).thenReturn(Mono.error(error));

    StepVerifier.create(service.submit(request)).expectErrorSatisfies(result ->
        assertSame(error, result)).verify();

    verify(orderOutbox).save(convertedOrder);
    verify(inventoryService).release(Mockito.anyString());
  }

  @Test
  public void shouldSubscribeToColdPublishersOnlyOnce() {
    Mono<EmailOrder> request = Mono.just(new EmailOrder());
    TacoOrder convertedOrder = new TacoOrder();
    TacoOrder savedOrder = new TacoOrder();
    AtomicInteger conversions = new AtomicInteger();
    AtomicInteger saves = new AtomicInteger();
    when(emailOrderService.convertEmailOrderToDomainOrder(request)).thenReturn(Mono.defer(() -> {
      conversions.incrementAndGet();
      return Mono.just(convertedOrder);
    }));
    when(orderOutbox.save(convertedOrder)).thenReturn(Mono.defer(() -> {
      saves.incrementAndGet();
      return Mono.just(savedOrder);
    }));

    Mono<TacoOrder> result = service.submit(request);
    assertEquals(0, conversions.get());
    assertEquals(0, saves.get());
    StepVerifier.create(result).expectNext(savedOrder).verifyComplete();
    assertEquals(1, conversions.get());
    assertEquals(1, saves.get());
    verify(orderOutbox, times(1)).save(convertedOrder);
  }
}
