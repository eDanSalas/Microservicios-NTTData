package tacos.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.Taco;
import tacos.User;
import tacos.data.RatingScoreCount;
import tacos.data.TacoRatingAggregate;
import tacos.data.TacoRatingRepository;
import tacos.data.TacoRepository;

@Service
public class TacoRatingService {
  private final TacoRatingRepository ratingRepo;
  private final TacoRepository tacoRepo;
  private final int minimumVotes;
  private final int maximumLimit;

  public TacoRatingService(TacoRatingRepository ratingRepo, TacoRepository tacoRepo,
      @Value("${tacocloud.ratings.minimum-votes:3}") int minimumVotes,
      @Value("${tacocloud.ratings.maximum-limit:50}") int maximumLimit) {
    this.ratingRepo = ratingRepo;
    this.tacoRepo = tacoRepo;
    this.minimumVotes = minimumVotes;
    this.maximumLimit = maximumLimit;
  }

  public Mono<TacoRatingStats> rate(User user, String tacoId, int score) {
    if (score < 1 || score > 5) return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
        "Score must be between 1 and 5"));
    String userId = userId(user);
    return publishedTaco(tacoId).flatMap(taco -> ratingRepo.upsert(userId, tacoId, score)
        .onErrorResume(DuplicateKeyException.class,
            exception -> ratingRepo.update(userId, tacoId, score))
        .then(ratingRepo.summarize(tacoId)).map(aggregate -> stats(taco, aggregate)));
  }

  public Mono<TacoRatingStats> statistics(String tacoId) {
    return publishedTaco(tacoId).flatMap(taco -> ratingRepo.summarize(tacoId)
        .map(aggregate -> stats(taco, aggregate)).defaultIfEmpty(empty(taco)));
  }

  public Flux<TacoRatingStats> top(int limit) {
    if (limit < 1) return Flux.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
        "Limit must be positive"));
    return ratingRepo.top(minimumVotes, Math.min(limit, maximumLimit))
        .map(aggregate -> stats(aggregate.getTaco(), aggregate));
  }

  private Mono<Taco> publishedTaco(String tacoId) {
    return tacoRepo.findById(tacoId).filter(Taco::isPublished).switchIfEmpty(Mono.error(
        new ResponseStatusException(HttpStatus.NOT_FOUND, "Published taco not found")));
  }

  private TacoRatingStats stats(Taco taco, TacoRatingAggregate aggregate) {
    Map<Integer, Long> distribution = distribution();
    if (aggregate.getDistribution() != null) for (RatingScoreCount score : aggregate.getDistribution())
      distribution.put(score.getScore(), score.getCount());
    return new TacoRatingStats(taco, BigDecimal.valueOf(aggregate.getAverage())
        .setScale(2, RoundingMode.HALF_UP), aggregate.getCount(), distribution);
  }

  private TacoRatingStats empty(Taco taco) {
    return new TacoRatingStats(taco, BigDecimal.ZERO.setScale(2), 0, distribution());
  }

  private Map<Integer, Long> distribution() {
    Map<Integer, Long> result = new LinkedHashMap<>();
    for (int score = 1; score <= 5; score++) result.put(score, 0L);
    return result;
  }

  private String userId(User user) {
    if (user == null || user.getId() == null || user.getId().isBlank()) throw new ResponseStatusException(
        HttpStatus.UNAUTHORIZED, "Authentication is required");
    return user.getId();
  }
}
