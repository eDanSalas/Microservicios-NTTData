package tacos.data;

import java.util.Date;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import reactor.core.publisher.Mono;
import tacos.OrderStatus;
import tacos.OrderStatusChange;
import tacos.TacoOrder;

public class KitchenQueueRepositoryImpl implements KitchenQueueRepository {
  private final ReactiveMongoTemplate template;

  public KitchenQueueRepositoryImpl(ReactiveMongoTemplate template) {
    this.template = template;
  }

  @Override
  public Mono<TacoOrder> claimNext(String stationId, String cookId, Date acceptedAt,
      OrderStatusChange change) {
    Query query = Query.query(Criteria.where("status").is(OrderStatus.CREATED))
        .with(Sort.by(Sort.Direction.ASC, "placedAt").and(Sort.by(Sort.Direction.ASC, "id")));
    Update update = new Update().set("status", OrderStatus.ACCEPTED)
        .set("stationId", stationId).set("stationActive", true).set("cookId", cookId)
        .set("acceptedAt", acceptedAt)
        .push("statusHistory", change).inc("version", 1);
    return template.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true),
        TacoOrder.class);
  }
}
