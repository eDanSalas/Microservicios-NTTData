package tacos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.Taco;
import tacos.TacoRating;
import tacos.User;
import tacos.data.RatingScoreCount;
import tacos.data.TacoRatingAggregate;
import tacos.data.TacoRatingRepository;
import tacos.data.TacoRepository;

public class TacoRatingServiceTest {
  private TacoRatingRepository ratingRepo;
  private TacoRepository tacoRepo;
  private TacoRatingService service;
  private User user;

  @BeforeEach
  public void setUp() {
    ratingRepo = Mockito.mock(TacoRatingRepository.class);
    tacoRepo = Mockito.mock(TacoRepository.class);
    service = new TacoRatingService(ratingRepo, tacoRepo, 2, 20);
    user = Mockito.mock(User.class);
    when(user.getId()).thenReturn("user-a");
  }

  @Test
  public void shouldUpsertRatingUsingAuthenticatedUser() {
    Taco taco = taco("taco-1", true);
    when(tacoRepo.findById("taco-1")).thenReturn(Mono.just(taco));
    when(ratingRepo.upsert("user-a", "taco-1", 5))
        .thenReturn(Mono.just(new TacoRating("user-a", "taco-1", 5)));
    when(ratingRepo.summarize("taco-1")).thenReturn(Mono.just(aggregate(taco, 4.666, 3, 5, 2)));

    TacoRatingStats result = service.rate(user, "taco-1", 5).block();

    assertEquals(new BigDecimal("4.67"), result.getAverage());
    assertEquals(3, result.getCount());
    assertEquals(2, result.getDistribution().get(5));
    verify(ratingRepo).upsert("user-a", "taco-1", 5);
  }

  @Test
  public void shouldRetryConcurrentUpsertAsUpdate() {
    Taco taco = taco("taco-1", true);
    when(tacoRepo.findById("taco-1")).thenReturn(Mono.just(taco));
    when(ratingRepo.upsert("user-a", "taco-1", 4))
        .thenReturn(Mono.error(new DuplicateKeyException("duplicate")));
    when(ratingRepo.update("user-a", "taco-1", 4))
        .thenReturn(Mono.just(new TacoRating("user-a", "taco-1", 4)));
    when(ratingRepo.summarize("taco-1")).thenReturn(Mono.just(aggregate(taco, 4, 1, 4, 1)));

    StepVerifier.create(service.rate(user, "taco-1", 4)).expectNextCount(1).verifyComplete();
    verify(ratingRepo).update("user-a", "taco-1", 4);
  }

  @Test
  public void shouldRejectInvalidScore() {
    StepVerifier.create(service.rate(user, "taco-1", 6))
        .expectErrorMatches(error -> error instanceof ResponseStatusException
            && ((ResponseStatusException) error).getStatus().value() == 400)
        .verify();
    verify(tacoRepo, never()).findById("taco-1");
  }

  @Test
  public void shouldRejectMissingOrUnpublishedTaco() {
    when(tacoRepo.findById("missing")).thenReturn(Mono.empty());
    when(tacoRepo.findById("draft")).thenReturn(Mono.just(taco("draft", false)));

    StepVerifier.create(service.rate(user, "missing", 4)).expectError(ResponseStatusException.class)
        .verify();
    StepVerifier.create(service.rate(user, "draft", 4)).expectError(ResponseStatusException.class)
        .verify();
    verify(ratingRepo, never()).upsert(Mockito.anyString(), Mockito.anyString(), Mockito.anyInt());
  }

  @Test
  public void shouldApplyMinimumVotesAndMaximumLimitToRanking() {
    Taco taco = taco("taco-1", true);
    when(ratingRepo.top(2, 20)).thenReturn(Flux.just(aggregate(taco, 4.5, 2, 5, 1)));

    List<TacoRatingStats> result = service.top(100).collectList().block();

    assertEquals(List.of("taco-1"), result.stream().map(item -> item.getTaco().getId()).toList());
    verify(ratingRepo).top(2, 20);
  }

  @Test
  public void shouldReturnZeroedDistributionWithoutVotes() {
    Taco taco = taco("taco-1", true);
    when(tacoRepo.findById("taco-1")).thenReturn(Mono.just(taco));
    when(ratingRepo.summarize("taco-1")).thenReturn(Mono.empty());

    TacoRatingStats result = service.statistics("taco-1").block();

    assertEquals(new BigDecimal("0.00"), result.getAverage());
    assertEquals(0, result.getCount());
    assertEquals(5, result.getDistribution().size());
  }

  private TacoRatingAggregate aggregate(Taco taco, double average, long count, int score,
      long scoreCount) {
    RatingScoreCount item = new RatingScoreCount();
    item.setScore(score);
    item.setCount(scoreCount);
    TacoRatingAggregate result = new TacoRatingAggregate();
    result.setTacoId(taco.getId());
    result.setTaco(taco);
    result.setAverage(average);
    result.setCount(count);
    result.setDistribution(List.of(item));
    return result;
  }

  private Taco taco(String id, boolean published) {
    Taco taco = new Taco();
    taco.setId(id);
    taco.setName("Rated taco");
    taco.setPublished(published);
    taco.setIngredients(List.of());
    return taco;
  }
}
