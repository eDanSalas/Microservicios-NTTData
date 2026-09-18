package tacos.web.api;

import java.net.URI;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.data.IngredientRepository;
import tacos.web.api.dto.IngredientRequest;
import tacos.web.api.dto.IngredientResponse;
import tacos.web.api.mapper.IngredientMapper;

@RestController
@RequestMapping(
    path = "/api/ingredients",
    produces = "application/json")
@CrossOrigin(origins = "http://localhost:8080")
public class IngredientController {

  private final IngredientRepository repo;
  private final IngredientMapper mapper;

  public IngredientController(
      IngredientRepository repo,
      IngredientMapper mapper) {

    this.repo = repo;
    this.mapper = mapper;
  }

  @GetMapping
  public Flux<IngredientResponse> allIngredients() {
    return repo.findAll()
        .map(mapper::toResponse);
  }

  @GetMapping("/{id}")
    public Mono<IngredientResponse> byId(@PathVariable String id) {
        return repo.findById(id)
            .switchIfEmpty(
                Mono.error(
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ingredient not found: " + id)))
            .map(mapper::toResponse);
    }

  @PutMapping("/{id}")
    public Mono<ResponseEntity<IngredientResponse>> updateIngredient(@PathVariable String id, @Valid @RequestBody IngredientRequest request) {
        return repo.findById(id)
            .switchIfEmpty(
                Mono.error(
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ingredient not found: " + id)))
            .flatMap(ingredient -> {
                mapper.updateEntity(
                    request,
                    ingredient);

                return repo.save(ingredient);
            })
            .map(saved ->
                ResponseEntity.ok(
                    mapper.toResponse(saved)));
    }

  @PostMapping
  public Mono<ResponseEntity<IngredientResponse>>
      postIngredient(
          @Valid @RequestBody
              IngredientRequest request,
          ServerHttpRequest serverRequest) {

    return repo.save(
            mapper.toEntity(request))
        .map(saved -> {
          URI location =
              UriComponentsBuilder
                  .fromUri(serverRequest.getURI())
                  .pathSegment(saved.getId())
                  .build()
                  .toUri();

          return ResponseEntity
              .created(location)
              .body(
                  mapper.toResponse(saved));
        });
  }

  @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteIngredient(@PathVariable String id) {
        return repo.findById(id)
            .switchIfEmpty(
                Mono.error(
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ingredient not found: " + id)))
            .flatMap(ingredient ->
                repo.delete(ingredient)
                    .thenReturn(
                        ResponseEntity
                            .noContent()
                            .build()));
    }
}