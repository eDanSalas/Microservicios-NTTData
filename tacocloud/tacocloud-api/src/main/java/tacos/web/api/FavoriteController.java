package tacos.web.api;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;
import tacos.User;
import tacos.service.FavoriteService;
import tacos.web.api.dto.FavoritePageResponse;
import tacos.web.api.dto.TacoResponse;
import tacos.web.api.mapper.TacoMapper;

@RestController
@RequestMapping(path = "/api/users/me/favorites", produces = "application/json")
public class FavoriteController {
  private final FavoriteService service;
  private final TacoMapper mapper;

  public FavoriteController(FavoriteService service, TacoMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  @PutMapping("/{tacoId}")
  public Mono<TacoResponse> add(@PathVariable String tacoId, @AuthenticationPrincipal User user) {
    return service.add(user, tacoId).map(mapper::toResponse);
  }

  @DeleteMapping("/{tacoId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> remove(@PathVariable String tacoId, @AuthenticationPrincipal User user) {
    return service.remove(user, tacoId);
  }

  @GetMapping
  public Mono<FavoritePageResponse> findAll(@AuthenticationPrincipal User user,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return service.findAll(user, page, size).map(result -> new FavoritePageResponse(
        result.getContent().stream().map(mapper::toResponse).toList(), result.getPage(),
        result.getSize(), result.getTotalElements(), result.getTotalPages()));
  }
}
