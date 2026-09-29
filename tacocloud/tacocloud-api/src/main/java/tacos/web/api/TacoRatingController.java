package tacos.web.api;

import javax.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.User;
import tacos.service.TacoRatingService;
import tacos.service.TacoRatingStats;
import tacos.web.api.dto.TacoRatingRequest;
import tacos.web.api.dto.TacoRatingResponse;
import tacos.web.api.mapper.TacoMapper;

@RestController
@RequestMapping(path = "/api/tacos", produces = "application/json")
public class TacoRatingController {
  private final TacoRatingService service;
  private final TacoMapper mapper;

  public TacoRatingController(TacoRatingService service, TacoMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  @PutMapping(path = "/{id}/rating", consumes = "application/json")
  public Mono<TacoRatingResponse> rate(@PathVariable String id,
      @Valid @RequestBody TacoRatingRequest request, @AuthenticationPrincipal User user) {
    return service.rate(user, id, request.getScore()).map(this::response);
  }

  @GetMapping("/{id}/rating")
  public Mono<TacoRatingResponse> statistics(@PathVariable String id) {
    return service.statistics(id).map(this::response);
  }

  @GetMapping("/top")
  public Flux<TacoRatingResponse> top(@RequestParam(defaultValue = "10") int limit) {
    return service.top(limit).map(this::response);
  }

  private TacoRatingResponse response(TacoRatingStats stats) {
    return new TacoRatingResponse(mapper.toResponse(stats.getTaco()), stats.getAverage(),
        stats.getCount(), stats.getDistribution());
  }
}
