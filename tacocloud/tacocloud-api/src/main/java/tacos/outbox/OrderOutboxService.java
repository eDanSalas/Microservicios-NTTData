package tacos.outbox;

import java.time.Clock;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;

import reactor.core.publisher.Mono;
import tacos.TacoOrder;
import tacos.data.OrderRepository;
import tacos.data.OutboxEvent;
import tacos.data.OutboxEventRepository;
import tacos.observability.CorrelationIds;
import tacos.service.OrderEventFactory;

@Service
public class OrderOutboxService {

  private final OrderRepository orderRepo;
  private final OutboxEventRepository outboxRepo;
  private final OrderEventFactory eventFactory;
  private final TransactionalOperator transactions;
  private final Clock clock;

  public OrderOutboxService(OrderRepository orderRepo, OutboxEventRepository outboxRepo,
      OrderEventFactory eventFactory, TransactionalOperator transactions,
      @Qualifier("outboxClock") Clock clock) {
    this.orderRepo = orderRepo;
    this.outboxRepo = outboxRepo;
    this.eventFactory = eventFactory;
    this.transactions = transactions;
    this.clock = clock;
  }

  public Mono<TacoOrder> save(TacoOrder order) {
    return Mono.deferContextual(context -> orderRepo.save(order).flatMap(saved -> outboxRepo.save(
        OutboxEvent.pending(eventFactory.created(saved, CorrelationIds.current(context)),
            Instant.now(clock))).thenReturn(saved))).as(transactions::transactional);
  }
}
