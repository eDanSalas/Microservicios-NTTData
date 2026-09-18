package tacos.web.api.dto;

import java.math.BigDecimal;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tacos.Ingredient.Type;
import tacos.Allergen;
import tacos.DietaryTag;
import tacos.SpiceLevel;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngredientResponse {

  private String id;
  private String name;
  private Type type;
  private BigDecimal unitPrice;
  private boolean available;
  private Set<DietaryTag> dietaryTags;
  private Set<Allergen> allergens;
  private SpiceLevel spiceLevel;
}
