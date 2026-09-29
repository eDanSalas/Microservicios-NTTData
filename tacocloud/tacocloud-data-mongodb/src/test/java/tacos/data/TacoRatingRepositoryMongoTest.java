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

import tacos.Taco;
import tacos.TacoRating;

@DataMongoTest(properties = {"spring.mongodb.embedded.version=4.0.12",
    "spring.data.mongodb.auto-index-creation=true"})
@EnabledIfSystemProperty(named = "tacocloud.mongo.integration", matches = "true")
public class TacoRatingRepositoryMongoTest {
  @SpringBootConfiguration
  @EnableAutoConfiguration
  static class TestApplication {
  }

  @Autowired
  private TacoRatingRepository repository;

  @Autowired
  private ReactiveMongoTemplate template;

  @BeforeEach
  public void clean() {
    template.remove(TacoRating.class).all().then(template.remove(Taco.class).all()).block();
  }

  @Test
  public void shouldUpsertOneRatingAndUpdateItsScore() {
    template.save(taco("taco-1", true)).block();

    repository.upsert("user-a", "taco-1", 2)
        .then(repository.upsert("user-a", "taco-1", 5)).block();

    assertEquals(1, repository.count().block());
    assertEquals(5, template.findAll(TacoRating.class).blockFirst().getScore());
    assertTrue(template.indexOps(TacoRating.class).getIndexInfo().map(info -> info.getName())
        .collectList().block().contains("uk_rating_user_taco"));
  }

  @Test
  public void shouldRankByAverageVotesAndStableId() {
    template.insertAll(List.of(taco("a", true), taco("b", true), taco("c", true),
        taco("draft", false))).then().block();
    rate("a", 5, 4);
    rate("b", 5, 4, 5, 4);
    rate("c", 5);
    rate("draft", 5, 5);

    List<TacoRatingAggregate> ranking = repository.top(2, 10).collectList().block();

    assertEquals(List.of("b", "a"), ranking.stream().map(TacoRatingAggregate::getTacoId).toList());
    assertEquals(4.5, ranking.get(0).getAverage());
    assertEquals(4, ranking.get(0).getCount());
    assertEquals(2, ranking.get(0).getDistribution().stream()
        .filter(item -> item.getScore() == 5).findFirst().get().getCount());
  }

  private void rate(String tacoId, int... scores) {
    for (int index = 0; index < scores.length; index++)
      repository.upsert(tacoId + "-user-" + index, tacoId, scores[index]).block();
  }

  private Taco taco(String id, boolean published) {
    Taco taco = new Taco();
    taco.setId(id);
    taco.setName("Rated " + id);
    taco.setPublished(published);
    taco.setIngredients(List.of());
    return taco;
  }
}
