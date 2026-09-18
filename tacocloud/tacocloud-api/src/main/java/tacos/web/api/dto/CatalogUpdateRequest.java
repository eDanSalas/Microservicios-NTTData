package tacos.web.api.dto;

import java.math.BigDecimal;
import java.util.Set;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import lombok.Data;
import tacos.Allergen;
import tacos.DietaryTag;
import tacos.SpiceLevel;

@Data
public class CatalogUpdateRequest {

  @DecimalMin("0.00")
  private BigDecimal unitPrice;
  private Boolean available;
  @Min(0)
  private Integer reorderLevel;
  private Set<DietaryTag> dietaryTags;
  private Set<Allergen> allergens;
  private SpiceLevel spiceLevel;
  @NotNull
  @Min(0)
  private Long version;
}
