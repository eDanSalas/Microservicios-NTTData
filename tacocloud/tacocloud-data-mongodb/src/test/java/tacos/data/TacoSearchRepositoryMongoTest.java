package tacos.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

import tacos.Allergen;
import tacos.DietaryTag;
import tacos.Ingredient;
import tacos.Ingredient.Type;
import tacos.SpiceLevel;
import tacos.Taco;

@DataMongoTest(properties = {"spring.mongodb.embedded.version=4.0.28",
    "spring.data.mongodb.auto-index-creation=true"})
@EnabledIfSystemProperty(named = "tacocloud.mongo.integration", matches = "true")
public class TacoSearchRepositoryMongoTest {

  @SpringBootConfiguration
  @EnableAutoConfiguration
  static class TestApplication {
  }

  @Autowired
  private TacoRepository repository;

  @Autowired
  private ReactiveMongoTemplate template;

  @BeforeEach
  public void clean() {
    template.remove(Taco.class).all().block();
  }

  @Test
  public void shouldFilterPageAndCreateIndexes() {
    repository.saveAll(List.of(taco("1", "Alpha taco", 1, vegan("INGA", SpiceLevel.HOT)),
        taco("2", "Alpha taco", 1, vegan("INGB", SpiceLevel.MILD)),
        taco("3", "Other taco", 2, allergen("INGA", Allergen.PEANUT)))).then().block();
    TacoSearchQuery query = new TacoSearchQuery("Alpha", "INGA", DietaryTag.VEGAN,
        Allergen.PEANUT, SpiceLevel.HOT, 0, 1, "createdAt", Sort.Direction.DESC);

    TacoSearchPage result = repository.search(query).block();

    assertEquals(List.of("1"), result.getContent().stream().map(Taco::getId).toList());
    assertEquals(1, result.getTotalElements());
    assertTrue(template.indexOps(Taco.class).getIndexInfo().map(info -> info.getName())
        .collectList().block().contains("taco_created_at_id"));
  }

  private Taco taco(String id, String name, long date, Ingredient ingredient) {
    Taco taco = new Taco();
    taco.setId(id);
    taco.setName(name);
    taco.setCreatedAt(new Date(date));
    taco.setIngredients(List.of(ingredient));
    return taco;
  }

  private Ingredient vegan(String id, SpiceLevel spice) {
    Ingredient ingredient = new Ingredient(id, id, Type.VEGGIES);
    ingredient.setDietaryTags(Set.of(DietaryTag.VEGAN));
    ingredient.setSpiceLevel(spice);
    return ingredient;
  }

  private Ingredient allergen(String id, Allergen allergen) {
    Ingredient ingredient = vegan(id, SpiceLevel.HOT);
    ingredient.setAllergens(Set.of(allergen));
    return ingredient;
  }
}
