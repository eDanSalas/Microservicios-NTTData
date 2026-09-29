package tacos.data;

import org.springframework.data.domain.Sort;

import lombok.Value;
import tacos.Allergen;
import tacos.DietaryTag;
import tacos.SpiceLevel;

@Value
public class TacoSearchQuery {
  String name;
  String ingredientId;
  DietaryTag diet;
  Allergen excludeAllergen;
  SpiceLevel spice;
  int page;
  int size;
  String sortField;
  Sort.Direction direction;
}
