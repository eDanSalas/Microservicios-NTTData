package tacos.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

import tacos.Allergen;
import tacos.DietaryTag;
import tacos.SpiceLevel;
import tacos.data.TacoSearchQuery;

public class TacoSearchQueryFactoryTest {
  private final TacoSearchQueryFactory factory = new TacoSearchQueryFactory(50, 10);

  @Test
  public void shouldCreateEmptyQueryWithSafeDefaults() {
    TacoSearchQuery query = factory.create(null, null, null, null, null, 0, 20, "createdAt,desc");

    assertEquals(0, query.getPage());
    assertEquals(20, query.getSize());
    assertEquals("createdAt", query.getSortField());
    assertEquals(Sort.Direction.DESC, query.getDirection());
  }

  @Test
  public void shouldNormalizeFiltersAndCapSize() {
    TacoSearchQuery query = factory.create(" taco ", " INGA ", "vegetarian", "tree-nut", "hot",
        2, 200, "name,ASC");

    assertEquals("taco", query.getName());
    assertEquals("INGA", query.getIngredientId());
    assertEquals(DietaryTag.VEGETARIAN, query.getDiet());
    assertEquals(Allergen.TREE_NUT, query.getExcludeAllergen());
    assertEquals(SpiceLevel.HOT, query.getSpice());
    assertEquals(50, query.getSize());
  }

  @Test
  public void shouldRejectUnsafeQueries() {
    assertThrows(ResponseStatusException.class,
        () -> factory.create("12345678901", null, null, null, null, 0, 20, "createdAt,desc"));
    assertThrows(ResponseStatusException.class,
        () -> factory.create(null, null, null, null, null, -1, 20, "createdAt,desc"));
    assertThrows(ResponseStatusException.class,
        () -> factory.create(null, null, null, null, null, 0, 20, "price,asc"));
  }
}
