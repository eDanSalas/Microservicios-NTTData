package tacos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.IdempotencyRecord;
import tacos.TacoOrder;
import tacos.User;
import tacos.data.IdempotencyRecordRepository;
import tacos.data.OrderRepository;
import tacos.web.api.dto.OrderCreateRequest;
import tacos.web.api.dto.OrderItemRequest;
import tacos.web.api.dto.OrderTacoRequest;

public class IdempotentOrderServiceTest {

  private final Map<String, IdempotencyRecord> records = new ConcurrentHashMap<>();
  private final Map<String, TacoOrder> orders = new ConcurrentHashMap<>();
  private IdempotencyRecordRepository recordRepo;
  private OrderRepository orderRepo;
  private OrderCreationService orderCreation;
  private IdempotentOrderService service;

  @BeforeEach
  public void setUp() {
    recordRepo = Mockito.mock(IdempotencyRecordRepository.class);
    orderRepo = Mockito.mock(OrderRepository.class);
    orderCreation = Mockito.mock(OrderCreationService.class);
    when(recordRepo.save(Mockito.any())).thenAnswer(invocation -> Mono.defer(() -> {
      IdempotencyRecord record = invocation.getArgument(0);
      if (record.getVersion() == null && records.putIfAbsent(record.getId(), record) != null)
        return Mono.error(new DuplicateKeyException("duplicate"));
      record.setVersion(record.getVersion() == null ? 0L : record.getVersion() + 1);
      records.put(record.getId(), record);
      return Mono.just(record);
    }));
    when(recordRepo.findByUserIdAndKey(Mockito.anyString(), Mockito.anyString()))
        .thenAnswer(invocation -> Mono.defer(() -> Mono.justOrEmpty(records.values().stream()
            .filter(record -> record.getUserId().equals(invocation.getArgument(0))
                && record.getKey().equals(invocation.getArgument(1)))
            .findFirst())));
    when(recordRepo.findById(Mockito.anyString())).thenAnswer(invocation ->
        Mono.defer(() -> Mono.justOrEmpty(records.get(invocation.getArgument(0)))));
    when(orderRepo.findById(Mockito.anyString())).thenAnswer(invocation ->
        Mono.defer(() -> Mono.justOrEmpty(orders.get(invocation.getArgument(0)))));
    when(orderCreation.create(Mockito.any(), Mockito.any(), Mockito.anyString()))
        .thenAnswer(invocation -> Mono.delay(Duration.ofMillis(30)).map(tick -> {
          TacoOrder order = new TacoOrder();
          order.setId(invocation.getArgument(2));
          orders.put(order.getId(), order);
          return order;
        }));
    service = new IdempotentOrderService(recordRepo, orderRepo, orderCreation,
        new OrderRequestHasher(), Duration.ofHours(24), Duration.ofMinutes(10),
        Duration.ofSeconds(2), Duration.ofMillis(5));
  }

  @Test
  public void shouldReturnSameOrderForSequentialRetryWithoutRepeatingEffects() {
    User user = user("user-1");
    TacoOrder first = service.create(request("Taco uno"), user, "order-key-1").block();
    TacoOrder second = service.create(request("Taco uno"), user, "order-key-1").block();

    assertEquals(first.getId(), second.getId());
    verify(orderCreation, times(1)).create(Mockito.any(), Mockito.same(user), Mockito.anyString());
  }

  @Test
  public void shouldCreateOneOrderForConcurrentRetries() {
    User user = user("user-1");
    Mono<TacoOrder> first = service.create(request("Taco uno"), user, "order-key-2");
    Mono<TacoOrder> second = service.create(request("Taco uno"), user, "order-key-2");

    StepVerifier.create(Flux.merge(first, second).collectList())
        .assertNext(result -> {
          assertEquals(2, result.size());
          assertEquals(result.get(0).getId(), result.get(1).getId());
        }).verifyComplete();
    verify(orderCreation, times(1)).create(Mockito.any(), Mockito.same(user), Mockito.anyString());
  }

  @Test
  public void shouldRejectSameKeyWithDifferentPayload() {
    User user = user("user-1");
    service.create(request("Taco uno"), user, "order-key-3").block();

    StepVerifier.create(service.create(request("Taco dos"), user, "order-key-3"))
        .expectErrorSatisfies(error -> assertEquals(HttpStatus.CONFLICT,
            ((ResponseStatusException) error).getStatus())).verify();
    verify(orderCreation, times(1)).create(Mockito.any(), Mockito.same(user), Mockito.anyString());
  }

  @Test
  public void shouldScopeKeyByUser() {
    service.create(request("Taco uno"), user("user-1"), "shared-key").block();
    service.create(request("Taco uno"), user("user-2"), "shared-key").block();

    assertEquals(2, orders.size());
    verify(orderCreation, times(2)).create(Mockito.any(), Mockito.any(), Mockito.anyString());
  }

  private User user(String id) {
    User user = Mockito.mock(User.class);
    when(user.getId()).thenReturn(id);
    return user;
  }

  private OrderCreateRequest request(String tacoName) {
    OrderTacoRequest taco = new OrderTacoRequest();
    taco.setName(tacoName);
    taco.setIngredientIds(List.of("FLTO", "GRBF"));
    OrderItemRequest item = new OrderItemRequest();
    item.setTaco(taco);
    item.setQuantity(1);
    OrderCreateRequest request = new OrderCreateRequest();
    request.setDeliveryName("Daniel");
    request.setDeliveryStreet("Calle 1");
    request.setDeliveryCity("Guadalajara");
    request.setDeliveryState("Jalisco");
    request.setDeliveryZip("44100");
    request.setPaymentMethodId("payment-1");
    request.setItems(List.of(item));
    return request;
  }
}
