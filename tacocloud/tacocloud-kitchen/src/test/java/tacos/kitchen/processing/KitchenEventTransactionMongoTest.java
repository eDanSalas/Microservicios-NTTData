package tacos.kitchen.processing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import tacos.messaging.OrderEvent;
import tacos.messaging.OrderEventPayload;
import tacos.messaging.OrderEventType;

@DataMongoTest
@Import({KitchenProcessingConfiguration.class, KitchenEventTransaction.class})
@Testcontainers(disabledWithoutDocker = true)
public class KitchenEventTransactionMongoTest {

  @Container
  static final MongoDBContainer MONGO = new MongoDBContainer(
      DockerImageName.parse("mongo:4.4.2"));

  @SpringBootConfiguration
  @EnableAutoConfiguration
  static class TestApplication {
  }

  @DynamicPropertySource
  static void mongoProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.mongodb.uri", MONGO::getReplicaSetUrl);
  }

  @Autowired
  private KitchenEventTransaction transaction;
  @Autowired
  private ProcessedEventRepository processedEvents;
  @Autowired
  private KitchenOrderStateRepository orderStates;
  @Autowired
  private MongoTemplate mongo;

  @BeforeEach
  public void clean() {
    processedEvents.deleteAll();
    orderStates.deleteAll();
    mongo.indexOps(KitchenOrderState.class).ensureIndex(
        new Index().on("lastEventId", Sort.Direction.ASC).unique());
  }

  @Test
  public void shouldRollbackMarkerWhenDurableEffectFails() {
    String eventId = UUID.randomUUID().toString();
    KitchenOrderState existing = new KitchenOrderState("order-1");
    existing.apply(event(eventId, "order-1"), Instant.now());
    orderStates.save(existing);

    assertThrows(DataIntegrityViolationException.class,
        () -> transaction.apply(event(eventId, "order-2")));

    assertFalse(processedEvents.existsById(eventId));
    assertFalse(orderStates.existsById("order-2"));
  }

  private OrderEvent event(String eventId, String orderId) {
    return new OrderEvent(eventId, OrderEventType.ORDER_CREATED, OrderEvent.CURRENT_VERSION,
        Instant.now().toString(), UUID.randomUUID().toString(),
        new OrderEventPayload(orderId, "CREATED", null, null, List.of()));
  }
}
