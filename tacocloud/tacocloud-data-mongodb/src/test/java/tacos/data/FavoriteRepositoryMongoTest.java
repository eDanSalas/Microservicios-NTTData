package tacos.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import tacos.Favorite;

@DataMongoTest(properties = {"spring.mongodb.embedded.version=4.0.12",
    "spring.data.mongodb.auto-index-creation=true"})
@EnabledIfSystemProperty(named = "tacocloud.mongo.integration", matches = "true")
public class FavoriteRepositoryMongoTest {
  @SpringBootConfiguration
  @EnableAutoConfiguration
  static class TestApplication {
  }

  @Autowired
  private FavoriteRepository repository;

  @Autowired
  private ReactiveMongoTemplate template;

  @BeforeEach
  public void clean() {
    template.remove(Favorite.class).all().block();
  }

  @Test
  public void shouldEnforceUniqueFavoriteDuringConcurrentWrites() {
    Mono<Boolean> first = save("first").subscribeOn(Schedulers.parallel());
    Mono<Boolean> second = save("second").subscribeOn(Schedulers.parallel());
    List<Boolean> results = Flux.merge(first, second).collectList().block();

    assertEquals(1, results.stream().filter(Boolean::booleanValue).count());
    assertEquals(1, repository.count().block());
    assertTrue(template.indexOps(Favorite.class).getIndexInfo().map(info -> info.getName())
        .collectList().block().contains("uk_favorite_user_taco"));
  }

  private Mono<Boolean> save(String id) {
    Favorite favorite = new Favorite("user-a", "taco-1");
    favorite.setId(id);
    return repository.save(favorite).map(saved -> true).onErrorReturn(false);
  }
}
