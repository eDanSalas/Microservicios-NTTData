package tacos.inventory;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class InsufficientStockException extends ResponseStatusException {

  private static final long serialVersionUID = 1L;
  private final String ingredientId;

  public InsufficientStockException(String ingredientId) {
    super(HttpStatus.CONFLICT, "Insufficient stock for ingredient: " + ingredientId);
    this.ingredientId = ingredientId;
  }

  public String getIngredientId() {
    return ingredientId;
  }
}
