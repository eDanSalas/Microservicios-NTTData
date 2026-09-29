package tacos.data;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.Favorite;

public interface FavoriteRepository extends ReactiveCrudRepository<Favorite, String> {
  Mono<Favorite> findByUserIdAndTacoId(String userId, String tacoId);
  Flux<Favorite> findByUserIdOrderByCreatedAtDescIdDesc(String userId, Pageable pageable);
  Mono<Long> countByUserId(String userId);
  Mono<Long> deleteByUserIdAndTacoId(String userId, String tacoId);
}
