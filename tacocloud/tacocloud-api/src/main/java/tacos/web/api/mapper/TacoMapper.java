package tacos.web.api.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import tacos.Ingredient;
import tacos.Taco;
import tacos.service.TacoClassificationService;
import tacos.web.api.dto.TacoCreateRequest;
import tacos.web.api.dto.TacoResponse;

@Component
public class TacoMapper {

  private final IngredientMapper ingredientMapper;
  private final TacoClassificationService classificationService;

  public TacoMapper(IngredientMapper ingredientMapper, TacoClassificationService classificationService) {
    this.ingredientMapper = ingredientMapper;
    this.classificationService = classificationService;
  }

  public Taco toEntity(TacoCreateRequest request, List<Ingredient> ingredients) {
    Taco taco = new Taco();
    taco.setName(request.getName());
    taco.setIngredients(ingredients);
    return taco;
  }

  public TacoResponse toResponse(Taco taco) {
    return new TacoResponse(taco.getId(), taco.getName(), taco.getCreatedAt(), taco.getIngredients().stream()
        .map(ingredientMapper::toResponse).collect(Collectors.toList()), classificationService.classify(taco));
  }
}
