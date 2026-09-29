package tacos.web.api;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.Taco;
import tacos.data.TacoRepository;
import tacos.data.TacoSearchQuery;
import tacos.service.TacoDesignService;
import tacos.service.TacoClassificationService;
import tacos.service.TacoOfTheDayService;
import tacos.web.api.dto.TacoClassificationResponse;
import tacos.web.api.dto.TacoCreateRequest;
import tacos.web.api.dto.TacoPageResponse;
import tacos.web.api.dto.TacoOfTheDayResponse;
import tacos.web.api.dto.TacoResponse;
import tacos.web.api.dto.TacoValidationResponse;
import tacos.web.api.mapper.TacoMapper;

@RestController
@RequestMapping(path = "/api/tacos", produces = "application/json")
@CrossOrigin(origins="http://localhost:8080")
public class TacoController {
  private final TacoRepository tacoRepo;
  private final TacoMapper tacoMapper;
  private final TacoClassificationService classificationService;
  private final TacoDesignService designService;
  private final TacoSearchQueryFactory searchQueryFactory;
  private final TacoOfTheDayService tacoOfTheDayService;

  public TacoController(TacoRepository tacoRepo, TacoMapper tacoMapper,
      TacoClassificationService classificationService, TacoDesignService designService,
      TacoSearchQueryFactory searchQueryFactory, TacoOfTheDayService tacoOfTheDayService) {
    this.tacoRepo = tacoRepo;
    this.tacoMapper = tacoMapper;
    this.classificationService = classificationService;
    this.designService = designService;
    this.searchQueryFactory = searchQueryFactory;
    this.tacoOfTheDayService = tacoOfTheDayService;
  }

  @GetMapping("/today")
  public Mono<TacoOfTheDayResponse> tacoOfTheDay() {
    return tacoOfTheDayService.recommend().map(recommendation -> new TacoOfTheDayResponse(
        tacoMapper.toResponse(recommendation.getTaco()), recommendation.getDate(),
        recommendation.getReason()));
  }

  @GetMapping(params = "!recent")
  public Mono<TacoPageResponse> tacos(@RequestParam(required = false) String name,
      @RequestParam(required = false) String ingredientId, @RequestParam(required = false) String diet,
      @RequestParam(required = false) String excludeAllergen, @RequestParam(required = false) String spice,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "createdAt,desc") String sort) {
    TacoSearchQuery query = searchQueryFactory.create(name, ingredientId, diet, excludeAllergen, spice,
        page, size, sort);
    return tacoRepo.search(query).map(result -> new TacoPageResponse(result.getContent().stream()
        .map(tacoMapper::toResponse).toList(), result.getPage(), result.getSize(),
        result.getTotalElements(), result.getTotalPages(), query.getSortField() + ","
            + query.getDirection().name().toLowerCase()));
  }

  @GetMapping(params="recent")
  public Flux<TacoResponse> recentTacos() {
    return tacoRepo.findAll().take(12).map(tacoMapper::toResponse);
  }

  @PostMapping(consumes = "application/json")
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<TacoResponse> postTaco(@Valid @RequestBody TacoCreateRequest request) {
    return designService.resolveAndRequireValid(request.getName(), request.getIngredientIds())
        .flatMap(tacoRepo::save).map(tacoMapper::toResponse);
  }

  @PostMapping(path = "/validate", consumes = "application/json")
  public Mono<TacoValidationResponse> validate(@Valid @RequestBody TacoCreateRequest request) {
    return designService.validate(request.getName(), request.getIngredientIds());
  }

  @GetMapping("/{id}")
  public Mono<TacoResponse> tacoById(@PathVariable("id") String id) {
    return tacoRepo.findById(id)
        .switchIfEmpty(
            Mono.error(
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Taco not found: " + id)))
        .map(tacoMapper::toResponse);
  }

  @GetMapping("/{id}/classification")
  public Mono<TacoClassificationResponse> classification(@PathVariable String id) {
    return tacoRepo.findById(id).switchIfEmpty(Mono.error(new ResponseStatusException(
        HttpStatus.NOT_FOUND, "Taco not found: " + id))).map(classificationService::classify);
  }

}
