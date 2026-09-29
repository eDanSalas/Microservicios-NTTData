package tacos.data;

import java.util.Date;

import reactor.core.publisher.Mono;
import tacos.OrderStatusChange;
import tacos.TacoOrder;

public interface KitchenQueueRepository {
  Mono<TacoOrder> claimNext(String stationId, String cookId, Date acceptedAt,
      OrderStatusChange change);
}
