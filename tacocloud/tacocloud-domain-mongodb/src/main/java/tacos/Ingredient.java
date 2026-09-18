package tacos;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor(access=AccessLevel.PUBLIC, force=true)
@Document
public class Ingredient {

  @Id
  private String id;
  @NotBlank
  private String name;
  private Type type;
  @DecimalMin("0.00")
  private BigDecimal unitPrice = BigDecimal.ZERO.setScale(2);
  private boolean available;
  @Min(0)
  private int stockOnHand;
  @Min(0)
  private int reorderLevel;
  @Version
  private Long version;
  private Set<DietaryTag> dietaryTags = new HashSet<>();
  private Set<Allergen> allergens = new HashSet<>();
  private SpiceLevel spiceLevel = SpiceLevel.NONE;

  public Ingredient(String id, String name, Type type, BigDecimal unitPrice, boolean available,
      int stockOnHand, int reorderLevel, Long version) {
    this(id, name, type, unitPrice, available, stockOnHand, reorderLevel, version,
        new HashSet<>(), new HashSet<>(), SpiceLevel.NONE);
  }

  public Ingredient(String id, String name, Type type) {
    this(id, name, type, BigDecimal.ZERO.setScale(2), false, 0, 0, null);
  }

  public void setUnitPrice(BigDecimal unitPrice) {
    this.unitPrice = unitPrice == null ? null : unitPrice.setScale(2, RoundingMode.HALF_UP);
  }

  public enum Type {
    WRAP, BOWL, PROTEIN, VEGGIES, CHEESE, SAUCE, BEVERAGE
  }

}
