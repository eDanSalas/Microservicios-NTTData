package tacos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.Favorite;
import tacos.Taco;
import tacos.User;
import tacos.data.FavoriteRepository;
import tacos.data.TacoRepository;

public class FavoriteServiceTest {
  private FavoriteRepository favoriteRepo;
  private TacoRepository tacoRepo;
  private FavoriteService service;
  private User user;

  @BeforeEach
  public void setUp() {
    favoriteRepo = Mockito.mock(FavoriteRepository.class);
    tacoRepo = Mockito.mock(TacoRepository.class);
    service = new FavoriteService(favoriteRepo, tacoRepo);
    user = user("user-a");
  }

  @Test
  public void shouldAddOnlyOneFavoriteForRepeatedPut() {
    Favorite favorite = new Favorite("user-a", "taco-1");
    when(tacoRepo.findById("taco-1")).thenReturn(Mono.just(taco("taco-1")));
    when(favoriteRepo.findByUserIdAndTacoId("user-a", "taco-1"))
        .thenReturn(Mono.empty(), Mono.just(favorite));
    when(favoriteRepo.save(any(Favorite.class))).thenReturn(Mono.just(favorite));

    StepVerifier.create(service.add(user, "taco-1").then(service.add(user, "taco-1")))
        .expectNextMatches(result -> result.getId().equals("taco-1")).verifyComplete();
    verify(favoriteRepo, times(1)).save(any(Favorite.class));
  }

  @Test
  public void shouldTreatConcurrentDuplicateAsIdempotentSuccess() {
    when(tacoRepo.findById("taco-1")).thenReturn(Mono.just(taco("taco-1")));
    when(favoriteRepo.findByUserIdAndTacoId("user-a", "taco-1")).thenReturn(Mono.empty());
    when(favoriteRepo.save(any(Favorite.class)))
        .thenReturn(Mono.error(new DuplicateKeyException("duplicate")));

    StepVerifier.create(service.add(user, "taco-1"))
        .expectNextMatches(result -> result.getId().equals("taco-1")).verifyComplete();
  }

  @Test
  public void shouldIsolateFavoritesByAuthenticatedUser() {
    when(favoriteRepo.findByUserIdOrderByCreatedAtDescIdDesc(Mockito.eq("user-a"),
        any(Pageable.class)))
        .thenReturn(Flux.just(new Favorite("user-a", "taco-a")));
    when(favoriteRepo.findByUserIdOrderByCreatedAtDescIdDesc(Mockito.eq("user-b"),
        any(Pageable.class)))
        .thenReturn(Flux.just(new Favorite("user-b", "taco-b")));
    when(favoriteRepo.countByUserId("user-a")).thenReturn(Mono.just(1L));
    when(favoriteRepo.countByUserId("user-b")).thenReturn(Mono.just(1L));
    when(tacoRepo.findById("taco-a")).thenReturn(Mono.just(taco("taco-a")));
    when(tacoRepo.findById("taco-b")).thenReturn(Mono.just(taco("taco-b")));

    assertEquals(List.of("taco-a"), service.findAll(user, 0, 20).block().getContent().stream()
        .map(Taco::getId).toList());
    assertEquals(List.of("taco-b"), service.findAll(user("user-b"), 0, 20).block().getContent()
        .stream().map(Taco::getId).toList());
  }

  @Test
  public void shouldDeleteIdempotently() {
    when(favoriteRepo.deleteByUserIdAndTacoId("user-a", "taco-1"))
        .thenReturn(Mono.just(1L), Mono.just(0L));

    StepVerifier.create(service.remove(user, "taco-1").then(service.remove(user, "taco-1")))
        .verifyComplete();
    verify(favoriteRepo, times(2)).deleteByUserIdAndTacoId("user-a", "taco-1");
  }

  @Test
  public void shouldRejectMissingTacoAndRemoveOrphan() {
    when(tacoRepo.findById("missing")).thenReturn(Mono.empty());
    when(favoriteRepo.deleteByUserIdAndTacoId("user-a", "missing")).thenReturn(Mono.just(1L));

    StepVerifier.create(service.add(user, "missing"))
        .expectErrorMatches(error -> error instanceof ResponseStatusException
            && ((ResponseStatusException) error).getStatus().value() == 404)
        .verify();
    verify(favoriteRepo, never()).save(any(Favorite.class));
  }

  @Test
  public void shouldRemoveOrphansFromFavoriteList() {
    Favorite orphan = new Favorite("user-a", "missing");
    Favorite valid = new Favorite("user-a", "taco-1");
    when(favoriteRepo.findByUserIdOrderByCreatedAtDescIdDesc(Mockito.eq("user-a"),
        any(Pageable.class)))
        .thenReturn(Flux.just(orphan, valid));
    when(tacoRepo.findById("missing")).thenReturn(Mono.empty());
    when(tacoRepo.findById("taco-1")).thenReturn(Mono.just(taco("taco-1")));
    when(favoriteRepo.delete(orphan)).thenReturn(Mono.empty());
    when(favoriteRepo.countByUserId("user-a")).thenReturn(Mono.just(1L));

    FavoritePage result = service.findAll(user, 0, 20).block();

    assertEquals(1, result.getTotalElements());
    assertEquals("taco-1", result.getContent().get(0).getId());
    verify(favoriteRepo).delete(orphan);
  }

  private User user(String id) {
    User result = Mockito.mock(User.class);
    when(result.getId()).thenReturn(id);
    return result;
  }

  private Taco taco(String id) {
    Taco taco = new Taco();
    taco.setId(id);
    taco.setName("Favorite taco");
    taco.setIngredients(List.of());
    return taco;
  }
}
