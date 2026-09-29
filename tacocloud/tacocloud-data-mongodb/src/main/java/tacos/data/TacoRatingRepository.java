package tacos.data;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import tacos.TacoRating;

public interface TacoRatingRepository extends ReactiveCrudRepository<TacoRating, String>,
    TacoRatingAggregationRepository {
}
