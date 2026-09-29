package tacos.service;

import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;
import tacos.Favorite;
import tacos.Taco;
import tacos.User;
import tacos.data.FavoriteRepository;
import tacos.data.TacoRepository;

@Service
public class FavoriteService {
  private final FavoriteRepository favoriteRepo;
  private final TacoRepository tacoRepo;

  public FavoriteService(FavoriteRepository favoriteRepo, TacoRepository tacoRepo) {
    this.favoriteRepo = favoriteRepo;
    this.tacoRepo = tacoRepo;
  }

  public Mono<Taco> add(User user, String tacoId) {
    String userId = userId(user);
    return tacoRepo.findById(tacoId).switchIfEmpty(Mono.defer(() -> removeOrphan(userId, tacoId)))
        .flatMap(taco -> favoriteRepo.findByUserIdAndTacoId(userId, tacoId)
            .switchIfEmpty(Mono.defer(() -> favoriteRepo.save(new Favorite(userId, tacoId))
                .onErrorResume(DuplicateKeyException.class, exception -> Mono.empty())))
            .thenReturn(taco));
  }

  public Mono<Void> remove(User user, String tacoId) {
    return favoriteRepo.deleteByUserIdAndTacoId(userId(user), tacoId).then();
  }

  public Mono<FavoritePage> findAll(User user, int page, int size) {
    if (page < 0 || size < 1) return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
        "Page must be non-negative and size must be positive"));
    int effectiveSize = Math.min(size, 50);
    String userId = userId(user);
    return favoriteRepo.findByUserIdOrderByCreatedAtDescIdDesc(userId,
        PageRequest.of(page, effectiveSize))
        .concatMap(favorite -> tacoRepo.findById(favorite.getTacoId())
            .switchIfEmpty(Mono.defer(() -> favoriteRepo.delete(favorite).then(Mono.empty()))))
        .collectList().flatMap(tacos -> favoriteRepo.countByUserId(userId)
            .map(total -> page(tacos, page, effectiveSize, total)));
  }

  private FavoritePage page(List<Taco> tacos, int page, int size, long total) {
    return new FavoritePage(tacos, page, size, total, (int) Math.ceil((double) total / size));
  }

  private Mono<Taco> removeOrphan(String userId, String tacoId) {
    return favoriteRepo.deleteByUserIdAndTacoId(userId, tacoId).then(Mono.error(
        new ResponseStatusException(HttpStatus.NOT_FOUND, "Taco not found")));
  }

  private String userId(User user) {
    if (user == null || user.getId() == null || user.getId().isBlank()) throw new ResponseStatusException(
        HttpStatus.UNAUTHORIZED, "Authentication is required");
    return user.getId();
  }
}
