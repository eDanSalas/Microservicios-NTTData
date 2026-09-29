package tacos.messaging;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class OrderEventPayload {
  private final String orderId;
  private final String status;
  private final String stationId;
  private final String cookId;
  private final List<Item> items;

  @JsonCreator
  public OrderEventPayload(@JsonProperty("orderId") String orderId,
      @JsonProperty("status") String status, @JsonProperty("stationId") String stationId,
      @JsonProperty("cookId") String cookId, @JsonProperty("items") List<Item> items) {
    this.orderId = Objects.requireNonNull(orderId, "orderId is required");
    this.status = Objects.requireNonNull(status, "status is required");
    this.stationId = stationId;
    this.cookId = cookId;
    this.items = items == null ? List.of() : List.copyOf(items);
  }

  public String getOrderId() {
    return orderId;
  }

  public String getStatus() {
    return status;
  }

  public String getStationId() {
    return stationId;
  }

  public String getCookId() {
    return cookId;
  }

  public List<Item> getItems() {
    return items;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static final class Item {
    private final String tacoName;
    private final int quantity;
    private final List<String> ingredients;

    @JsonCreator
    public Item(@JsonProperty("tacoName") String tacoName,
        @JsonProperty("quantity") int quantity,
        @JsonProperty("ingredients") List<String> ingredients) {
      this.tacoName = Objects.requireNonNull(tacoName, "tacoName is required");
      if (quantity < 1) throw new IllegalArgumentException("quantity must be positive");
      this.quantity = quantity;
      this.ingredients = ingredients == null ? List.of() : List.copyOf(ingredients);
    }

    public String getTacoName() {
      return tacoName;
    }

    public int getQuantity() {
      return quantity;
    }

    public List<String> getIngredients() {
      return ingredients;
    }
  }
}
