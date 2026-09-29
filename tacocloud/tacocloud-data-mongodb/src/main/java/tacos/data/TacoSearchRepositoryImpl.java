package tacos.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import reactor.core.publisher.Mono;
import tacos.DietaryTag;
import tacos.SpiceLevel;
import tacos.Taco;

public class TacoSearchRepositoryImpl implements TacoSearchRepository {
  private final ReactiveMongoTemplate template;

  public TacoSearchRepositoryImpl(ReactiveMongoTemplate template) {
    this.template = template;
  }

  @Override
  public Mono<TacoSearchPage> search(TacoSearchQuery query) {
    Query pageQuery = pagedQuery(query);
    return Mono.zip(template.find(pageQuery, Taco.class).collectList(), template.count(mongoQuery(query), Taco.class))
        .map(result -> new TacoSearchPage(result.getT1(), query.getPage(), query.getSize(), result.getT2(),
            (int) Math.ceil((double) result.getT2() / query.getSize())));
  }

  Query mongoQuery(TacoSearchQuery query) {
    List<Criteria> filters = new ArrayList<>();
    if (query.getName() != null) filters.add(Criteria.where("name")
        .regex(Pattern.compile(Pattern.quote(query.getName()), Pattern.CASE_INSENSITIVE)));
    if (query.getIngredientId() != null) filters.add(Criteria.where("ingredients.id").is(query.getIngredientId()));
    if (query.getDiet() != null) filters.add(diet(query.getDiet()));
    if (query.getExcludeAllergen() != null)
      filters.add(Criteria.where("ingredients.allergens").ne(query.getExcludeAllergen()));
    if (query.getSpice() != null) filters.add(spice(query.getSpice()));
    return filters.isEmpty() ? new Query() : new Query(new Criteria().andOperator(filters.toArray(new Criteria[0])));
  }

  Query pagedQuery(TacoSearchQuery query) {
    return mongoQuery(query).with(sort(query)).skip((long) query.getPage() * query.getSize())
        .limit(query.getSize());
  }

  private Criteria diet(DietaryTag tag) {
    List<DietaryTag> accepted = tag == DietaryTag.VEGETARIAN
        ? List.of(DietaryTag.VEGETARIAN, DietaryTag.VEGAN) : List.of(tag);
    Criteria missingTag = Criteria.where("dietaryTags").nin(accepted);
    Criteria allIngredients = Criteria.where("ingredients").not().elemMatch(missingTag);
    return tag == DietaryTag.GLUTEN_FREE ? new Criteria().andOperator(allIngredients,
        Criteria.where("ingredients.allergens").ne("GLUTEN")) : allIngredients;
  }

  private Criteria spice(SpiceLevel level) {
    List<String> higher = Arrays.stream(SpiceLevel.values()).filter(value -> value.compareTo(level) > 0)
        .map(Enum::name).collect(Collectors.toList());
    if (level == SpiceLevel.NONE) return Criteria.where("ingredients.spiceLevel").nin(higher);
    return new Criteria().andOperator(Criteria.where("ingredients.spiceLevel").is(level),
        Criteria.where("ingredients.spiceLevel").nin(higher));
  }

  private Sort sort(TacoSearchQuery query) {
    Sort primary = Sort.by(query.getDirection(), query.getSortField());
    return "id".equals(query.getSortField()) ? primary : primary.and(Sort.by(query.getDirection(), "id"));
  }
}
