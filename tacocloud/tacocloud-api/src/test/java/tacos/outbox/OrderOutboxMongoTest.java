package tacos.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.bson.Document;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import tacos.TacoOrder;
import tacos.data.OrderRepository;
import tacos.data.OutboxEvent;
import tacos.data.OutboxEventRepository;
import tacos.data.OutboxStatus;
import tacos.messaging.OrderEvent;
import tacos.messaging.OrderEventPayload;
import tacos.messaging.OrderEventType;
import tacos.service.OrderEventFactory;

@DataMongoTest(properties = "spring.data.mongodb.auto-index-creation=false",
    excludeAutoConfiguration = EmbeddedMongoAutoConfiguration.class)
@Import({OutboxConfiguration.class, OrderOutboxService.class})
@Testcontainers(disabledWithoutDocker = true)
public class OrderOutboxMongoTest {

  @Container
  static final MongoDBContainer MONGO = new MongoDBContainer(
      DockerImageName.parse("mongo:4.4.2"));

  @SpringBootConfiguration
  @EnableAutoConfiguration
  @EntityScan(basePackageClasses = TacoOrder.class)
  @EnableReactiveMongoRepositories(basePackageClasses = OrderRepository.class)
  static class TestApplication {
  }

  @DynamicPropertySource
  static void mongoProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.mongodb.uri", MONGO::getReplicaSetUrl);
  }

  @Autowired
  private OrderOutboxService orderOutbox;
  @Autowired
  private OrderRepository orderRepo;
  @Autowired
  private OutboxEventRepository outboxRepo;
  @Autowired
  private ReactiveMongoTemplate mongo;
  @MockBean
  private OrderEventFactory eventFactory;

  @BeforeEach
  public void clean() {
    // Materialize collections with acknowledged writes before any transaction.
    // This also cooperates with Spring's asynchronous automatic index creation.
    orderRepo.save(new TacoOrder())
        .then(outboxRepo.save(OutboxEvent.pending(event("fixture-order"), Instant.now())))
        .then(outboxRepo.deleteAll()).then(orderRepo.deleteAll()).block();
  }

  @Test
  public void shouldRollbackOrderWhenOutboxInsertFails() {
    OrderEvent event = event("rollback-order");
    when(eventFactory.created(any(), any())).thenReturn(event);
    outboxRepo.save(OutboxEvent.pending(event("existing-order"), Instant.now())).block();
    TacoOrder order = new TacoOrder();
    order.setId("rollback-order");

    // save() is an upsert: reusing an ID does not cause an insert failure.
    // Reject the synthetic order with a real MongoDB collection validator.
    mongo.executeCommand(new Document("collMod", "order_outbox").append("validator",
        new Document("event.payload.orderId", new Document("$ne", order.getId())))).block();
    try {
      StepVerifier.create(orderOutbox.save(order)).expectError().verify();
      StepVerifier.create(orderRepo.findById(order.getId())).verifyComplete();
      StepVerifier.create(outboxRepo.count()).expectNext(1L).verifyComplete();
    } finally {
      mongo.executeCommand(new Document("collMod", "order_outbox")
          .append("validator", new Document())).block();
    }
  }

  @Test
  public void shouldCommitOrderAndNewOutboxEventTogether() {
    OrderEvent event = event("committed-order");
    when(eventFactory.created(any(), any())).thenReturn(event);
    TacoOrder order = new TacoOrder();
    order.setId("committed-order");

    StepVerifier.create(orderOutbox.save(order)).expectNext(order).verifyComplete();

    StepVerifier.create(Mono.zip(orderRepo.findById(order.getId()),
        outboxRepo.findById(event.getEventId()))).assertNext(saved -> {
          assertNotNull(saved.getT1());
          assertEquals(OutboxStatus.NEW, saved.getT2().getStatus());
        }).verifyComplete();
  }

  @Test
  public void shouldAllowOnlyOnePublisherToClaimAnEvent() {
    Instant now = Instant.parse("2026-09-21T12:00:00Z");
    outboxRepo.save(OutboxEvent.pending(event("concurrent-order"), now)).block();
    Mono<OutboxEvent> first = claim(now).subscribeOn(Schedulers.parallel());
    Mono<OutboxEvent> second = claim(now).subscribeOn(Schedulers.parallel());

    List<OutboxEvent> claimed = Flux.merge(first, second).collectList().block();

    assertEquals(1, claimed.size());
    assertEquals(OutboxStatus.PUBLISHING, claimed.get(0).getStatus());
    assertEquals(1, claimed.get(0).getAttempts());
  }

  private Mono<OutboxEvent> claim(Instant now) {
    return outboxRepo.claimBatch(now, 1, 5, Duration.ofMinutes(1)).singleOrEmpty();
  }

  private OrderEvent event(String orderId) {
    return OrderEvent.create(OrderEventType.ORDER_CREATED, UUID.randomUUID().toString(),
        new OrderEventPayload(orderId, "CREATED", null, null, List.of()));
  }
}
