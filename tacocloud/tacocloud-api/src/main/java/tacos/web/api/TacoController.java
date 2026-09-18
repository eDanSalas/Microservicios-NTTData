package tacos.web.api;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.Taco;
import tacos.data.TacoRepository;
import tacos.service.TacoDesignService;
import tacos.service.TacoClassificationService;
import tacos.web.api.dto.TacoClassificationResponse;
import tacos.web.api.dto.TacoCreateRequest;
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

  public TacoController(TacoRepository tacoRepo, TacoMapper tacoMapper,
      TacoClassificationService classificationService, TacoDesignService designService) {
    this.tacoRepo = tacoRepo;
    this.tacoMapper = tacoMapper;
    this.classificationService = classificationService;
    this.designService = designService;
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
