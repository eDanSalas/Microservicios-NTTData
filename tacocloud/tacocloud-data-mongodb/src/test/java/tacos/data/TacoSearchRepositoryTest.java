package tacos.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;
import java.util.List;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import tacos.Allergen;
import tacos.DietaryTag;
import tacos.SpiceLevel;

public class TacoSearchRepositoryTest {
  private final TacoSearchRepositoryImpl repository =
      new TacoSearchRepositoryImpl(Mockito.mock(ReactiveMongoTemplate.class));

  @Test
  public void shouldCombineAllFiltersAndEscapeName() {
    Query query = repository.mongoQuery(new TacoSearchQuery("Taco.*", "INGA", DietaryTag.VEGAN,
        Allergen.PEANUT, SpiceLevel.HOT, 0, 20, "createdAt", Sort.Direction.DESC));
    String json = query.getQueryObject().toString();

    assertTrue(json.contains("ingredients.id"));
    assertTrue(json.contains("ingredients.allergens"));
    assertTrue(json.contains("ingredients.spiceLevel"));
    List<Document> filters = (List<Document>) query.getQueryObject().get("$and");
    Pattern regex = (Pattern) filters.stream().filter(filter -> filter.containsKey("name"))
        .findFirst().orElseThrow().get("name");
    assertEquals("\\QTaco.*\\E", regex.pattern());
  }

  @Test
  public void shouldBuildEmptyQuery() {
    Query query = repository.mongoQuery(new TacoSearchQuery(null, null, null, null, null,
        0, 20, "createdAt", Sort.Direction.DESC));

    assertTrue(query.getQueryObject().isEmpty());
  }

  @Test
  public void shouldApplyStablePagingOrder() {
    Query query = repository.pagedQuery(new TacoSearchQuery(null, null, null, null, null,
        2, 5, "name", Sort.Direction.ASC));

    assertEquals(10, query.getSkip());
    assertEquals(5, query.getLimit());
    assertEquals(new Document("name", 1).append("id", 1), query.getSortObject());
  }
}
