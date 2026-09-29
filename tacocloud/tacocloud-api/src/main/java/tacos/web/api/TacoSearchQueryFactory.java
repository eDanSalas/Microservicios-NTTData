package tacos.web.api;

import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import tacos.Allergen;
import tacos.DietaryTag;
import tacos.SpiceLevel;
import tacos.data.TacoSearchQuery;

@Component
public class TacoSearchQueryFactory {
  private static final Set<String> SORT_FIELDS = Set.of("createdAt", "name", "id");
  private final int maxSize;
  private final int maxNameLength;

  public TacoSearchQueryFactory(@Value("${tacocloud.taco-search.max-size:50}") int maxSize,
      @Value("${tacocloud.taco-search.max-name-length:60}") int maxNameLength) {
    this.maxSize = maxSize;
    this.maxNameLength = maxNameLength;
  }

  public TacoSearchQuery create(String name, String ingredientId, String diet, String excludeAllergen,
      String spice, int page, int size, String sort) {
    if (page < 0 || size < 1) throw badRequest("page must be non-negative and size must be positive");
    String normalizedName = text(name);
    if (normalizedName != null && normalizedName.length() > maxNameLength)
      throw badRequest("name must not exceed " + maxNameLength + " characters");
    String[] order = sort == null ? new String[] {"createdAt", "desc"} : sort.split(",", -1);
    if (order.length > 2 || !SORT_FIELDS.contains(order[0])) throw badRequest("unsupported sort field");
    Sort.Direction direction;
    try {
      direction = Sort.Direction.fromString(order.length == 1 ? "asc" : order[1]);
    } catch (IllegalArgumentException exception) {
      throw badRequest("sort direction must be asc or desc");
    }
    return new TacoSearchQuery(normalizedName, text(ingredientId), value(diet, DietaryTag.class),
        value(excludeAllergen, Allergen.class), value(spice, SpiceLevel.class), page,
        Math.min(size, maxSize), order[0], direction);
  }

  private String text(String value) {
    return value == null || value.trim().isEmpty() ? null : value.trim();
  }

  private <T extends Enum<T>> T value(String value, Class<T> type) {
    if (text(value) == null) return null;
    try {
      return Enum.valueOf(type, value.trim().replace('-', '_').toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      throw badRequest("invalid " + type.getSimpleName());
    }
  }

  private ResponseStatusException badRequest(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }
}
