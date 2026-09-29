package tacos.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import reactor.core.publisher.Mono;
import tacos.OrderStatus;
import tacos.OrderStatusChange;
import tacos.TacoOrder;

public class KitchenQueueRepositoryTest {
  @Test
  public void shouldUseAtomicFifoConditionalClaim() {
    ReactiveMongoTemplate template = mock(ReactiveMongoTemplate.class);
    TacoOrder claimed = new TacoOrder();
    when(template.findAndModify(any(Query.class), any(Update.class),
        any(FindAndModifyOptions.class), eq(TacoOrder.class))).thenReturn(Mono.just(claimed));
    KitchenQueueRepositoryImpl repository = new KitchenQueueRepositoryImpl(template);
    Date now = new Date();
    OrderStatusChange change = new OrderStatusChange(OrderStatus.CREATED, OrderStatus.ACCEPTED,
        now, "cook-1", "KITCHEN", "KITCHEN_CLAIM", "CLAIMED");

    assertEquals(claimed, repository.claimNext("station-1", "cook-1", now, change).block());

    ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
    ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
    verify(template).findAndModify(query.capture(), update.capture(),
        any(FindAndModifyOptions.class), eq(TacoOrder.class));
    assertEquals(OrderStatus.CREATED, query.getValue().getQueryObject().get("status"));
    assertEquals(new Document("placedAt", 1).append("id", 1), query.getValue().getSortObject());
    Document changes = update.getValue().getUpdateObject();
    assertEquals(OrderStatus.ACCEPTED, ((Document) changes.get("$set")).get("status"));
    assertEquals("station-1", ((Document) changes.get("$set")).get("stationId"));
    assertEquals(true, ((Document) changes.get("$set")).get("stationActive"));
    assertTrue(((Document) changes.get("$push")).containsKey("statusHistory"));
    assertEquals(1L, ((Number) ((Document) changes.get("$inc")).get("version")).longValue());
  }
}
