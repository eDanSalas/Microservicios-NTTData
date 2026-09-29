package tacos.data;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.TacoRating;

public interface TacoRatingAggregationRepository {
  Mono<TacoRating> upsert(String userId, String tacoId, int score);
  Mono<TacoRating> update(String userId, String tacoId, int score);
  Mono<TacoRatingAggregate> summarize(String tacoId);
  Flux<TacoRatingAggregate> top(int minimumVotes, int limit);
}
