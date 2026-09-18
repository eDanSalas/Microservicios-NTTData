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
import tacos.TacoOrder;
import tacos.InventoryReservation;
import tacos.data.OrderRepository;
import tacos.inventory.InventoryService;
import tacos.messaging.OrderMessagingService;
import tacos.web.api.EmailOrder;
import tacos.web.api.EmailOrderService;

public class EmailOrderSubmissionServiceTest {

  private EmailOrderService emailOrderService;
  private OrderRepository orderRepo;
  private OrderMessagingService orderMessages;
  private EmailOrderSubmissionService service;
  private InventoryService inventoryService;

  @BeforeEach
  public void setUp() {
    emailOrderService =
        Mockito.mock(EmailOrderService.class);

    orderRepo =
        Mockito.mock(OrderRepository.class);

    orderMessages =
        Mockito.mock(OrderMessagingService.class);

    inventoryService = Mockito.mock(InventoryService.class);
    when(inventoryService.reserve(Mockito.any(TacoOrder.class), Mockito.anyString()))
        .thenReturn(Mono.just(new InventoryReservation()));
    when(inventoryService.release(Mockito.anyString())).thenReturn(Mono.empty());

    service = new EmailOrderSubmissionService(
        emailOrderService,
        orderRepo,
        orderMessages,
        inventoryService);
  }

  @Test
  public void shouldConvertSaveThenPublish() {
    EmailOrder emailOrder = new EmailOrder();

    Mono<EmailOrder> request =
        Mono.just(emailOrder);

    TacoOrder convertedOrder =
        new TacoOrder();

    TacoOrder savedOrder =
        new TacoOrder();

    savedOrder.setId("order-1");

    when(emailOrderService
            .convertEmailOrderToDomainOrder(request))
        .thenReturn(Mono.just(convertedOrder));

    when(orderRepo.save(convertedOrder))
        .thenReturn(Mono.just(savedOrder));

    StepVerifier.create(
            service.submit(request))
        .assertNext(result ->
            assertSame(savedOrder, result))
        .verifyComplete();

    InOrder executionOrder =
        Mockito.inOrder(
            emailOrderService,
            orderRepo,
            orderMessages);

    executionOrder
        .verify(emailOrderService)
        .convertEmailOrderToDomainOrder(request);

    executionOrder
        .verify(orderRepo)
        .save(convertedOrder);

    executionOrder
        .verify(orderMessages)
        .sendOrder(savedOrder);
  }

  @Test
  public void shouldNotSaveOrPublishWhenConversionFails() {
    EmailOrder emailOrder = new EmailOrder();

    Mono<EmailOrder> request =
        Mono.just(emailOrder);

    RuntimeException conversionError =
        new RuntimeException(
            "Conversion failed");

    when(emailOrderService
            .convertEmailOrderToDomainOrder(request))
        .thenReturn(
            Mono.error(conversionError));

    StepVerifier.create(
            service.submit(request))
        .expectErrorSatisfies(error ->
            assertSame(conversionError, error))
        .verify();

    verify(emailOrderService)
        .convertEmailOrderToDomainOrder(request);

    verifyNoInteractions(
        orderRepo,
        orderMessages);
  }

  @Test
  public void shouldNotPublishWhenSaveFails() {
    EmailOrder emailOrder = new EmailOrder();

    Mono<EmailOrder> request =
        Mono.just(emailOrder);

    TacoOrder convertedOrder =
        new TacoOrder();

    RuntimeException saveError =
        new RuntimeException(
            "Database failed");

    when(emailOrderService
            .convertEmailOrderToDomainOrder(request))
        .thenReturn(Mono.just(convertedOrder));

    when(orderRepo.save(convertedOrder))
        .thenReturn(
            Mono.error(saveError));

    StepVerifier.create(
            service.submit(request))
        .expectErrorSatisfies(error ->
            assertSame(saveError, error))
        .verify();

    verify(orderRepo)
        .save(convertedOrder);

    verifyNoInteractions(orderMessages);
  }

  @Test
  public void shouldSubscribeToColdPublishersOnlyOnce() {
    EmailOrder emailOrder = new EmailOrder();

    Mono<EmailOrder> request =
        Mono.just(emailOrder);

    TacoOrder convertedOrder =
        new TacoOrder();

    TacoOrder savedOrder =
        new TacoOrder();

    savedOrder.setId("order-1");

    AtomicInteger conversionSubscriptions =
        new AtomicInteger();

    AtomicInteger saveSubscriptions =
        new AtomicInteger();

    AtomicInteger publications =
        new AtomicInteger();

    when(emailOrderService
            .convertEmailOrderToDomainOrder(request))
        .thenReturn(
            Mono.defer(() -> {
              conversionSubscriptions.incrementAndGet();
              return Mono.just(convertedOrder);
            }));

    when(orderRepo.save(convertedOrder))
        .thenReturn(
            Mono.defer(() -> {
              saveSubscriptions.incrementAndGet();
              return Mono.just(savedOrder);
            }));

    Mockito.doAnswer(invocation -> {
      publications.incrementAndGet();
      return null;
    })
        .when(orderMessages)
        .sendOrder(savedOrder);

    Mono<TacoOrder> result =
        service.submit(request);

    assertEquals(
        0,
        conversionSubscriptions.get());

    assertEquals(
        0,
        saveSubscriptions.get());

    assertEquals(
        0,
        publications.get());

    StepVerifier.create(result)
        .expectNext(savedOrder)
        .verifyComplete();

    assertEquals(
        1,
        conversionSubscriptions.get());

    assertEquals(
        1,
        saveSubscriptions.get());

    assertEquals(
        1,
        publications.get());

    verify(orderRepo, times(1))
        .save(convertedOrder);

    verify(orderMessages, times(1))
        .sendOrder(savedOrder);
  }
}
