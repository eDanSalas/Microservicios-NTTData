package tacos.data;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.OrderStatus;
import tacos.TacoOrder;

public interface OrderRepository extends ReactiveCrudRepository<TacoOrder, String>,
    KitchenQueueRepository {

  Flux<TacoOrder> findByUserIdOrderByPlacedAtDesc(String userId, Pageable pageable);
  Flux<TacoOrder> findByUserIdOrderByPlacedAtDesc(String userId);
  Flux<TacoOrder> findByUserId(String userId, Pageable pageable);
  Mono<Long> countByUserId(String userId);
  Mono<TacoOrder> findByIdAndUserId(String id, String userId);
  Flux<TacoOrder> findAllBy(Pageable pageable);
  Flux<TacoOrder> findByStatus(OrderStatus status, Pageable pageable);
  Mono<Long> countByStatus(OrderStatus status);
  Flux<TacoOrder> findByUserIdAndStatus(String userId, OrderStatus status, Pageable pageable);
  Mono<Long> countByUserIdAndStatus(String userId, OrderStatus status);
  Flux<TacoOrder> findByStatusOrderByPlacedAtAscIdAsc(OrderStatus status);

}
