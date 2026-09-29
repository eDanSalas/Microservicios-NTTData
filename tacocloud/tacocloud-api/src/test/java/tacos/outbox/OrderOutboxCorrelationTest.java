package tacos.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.transaction.reactive.TransactionalOperator;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.OrderStatus;
import tacos.TacoOrder;
import tacos.data.OrderRepository;
import tacos.data.OutboxEvent;
import tacos.data.OutboxEventRepository;
import tacos.observability.CorrelationIds;
import tacos.service.OrderEventFactory;

public class OrderOutboxCorrelationTest {

  @Test
  public void shouldStoreReactorCorrelationIdInsideOutboxEvent() {
    OrderRepository orders = Mockito.mock(OrderRepository.class);
    OutboxEventRepository outbox = Mockito.mock(OutboxEventRepository.class);
    TransactionalOperator transactions = Mockito.mock(TransactionalOperator.class);
    TacoOrder order = new TacoOrder();
    order.setId("order-1");
    order.setStatus(OrderStatus.CREATED);
    when(orders.save(order)).thenReturn(Mono.just(order));
    when(outbox.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    when(transactions.transactional(Mockito.<Mono<TacoOrder>>any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    OrderOutboxService service = new OrderOutboxService(orders, outbox,
        new OrderEventFactory(), transactions, Clock.systemUTC());

    String correlationId = UUID.randomUUID().toString();
    StepVerifier.create(service.save(order).contextWrite(context ->
        context.put(CorrelationIds.CONTEXT_KEY, correlationId)))
        .expectNext(order).verifyComplete();

    ArgumentCaptor<OutboxEvent> saved = ArgumentCaptor.forClass(OutboxEvent.class);
    Mockito.verify(outbox).save(saved.capture());
    assertEquals(correlationId, saved.getValue().getEvent().getCorrelationId());
  }
}
