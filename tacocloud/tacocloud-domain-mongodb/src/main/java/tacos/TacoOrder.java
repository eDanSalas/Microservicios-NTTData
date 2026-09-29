package tacos;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.ToString;

@Data
@Document
@CompoundIndexes({
    @CompoundIndex(name = "idx_order_user_history", def = "{'userId': 1, 'placedAt': -1, '_id': -1}"),
    @CompoundIndex(name = "idx_order_history", def = "{'placedAt': -1, '_id': -1}")
})
public class TacoOrder implements Serializable {
  private static final long serialVersionUID = 1L;

  @Id
  private String id;
  @Version
  private Long version;
  private Date placedAt = new Date();

  private OrderStatus status = OrderStatus.CREATED;
  private List<OrderStatusChange> statusHistory = new ArrayList<>();

  @Indexed(name = "uniq_active_kitchen_station", unique = true,
      partialFilter = "{ 'stationActive': true }")
  private String stationId;
  private boolean stationActive;
  private String cookId;
  private Date acceptedAt;

  @JsonIgnore
  @ToString.Exclude
  @Transient
  private User user;

  @JsonIgnore
  private String userId;

  private String deliveryName;

  private String deliveryStreet;

  private String deliveryCity;

  private String deliveryState;

  private String deliveryZip;

  @JsonIgnore
  @ToString.Exclude
  private String paymentMethodId;

  @JsonIgnore
  @ToString.Exclude
  private String paymentBrand;

  @JsonIgnore
  @ToString.Exclude
  private String paymentLast4;

  private List<OrderItem> items = new ArrayList<>();
  private BigDecimal subtotal = BigDecimal.ZERO.setScale(2);
  private BigDecimal discount = BigDecimal.ZERO.setScale(2);
  private BigDecimal total = BigDecimal.ZERO.setScale(2);
  private String currency = "MXN";

  @JsonIgnore
  private String couponCode;

  public void setUser(User user) {
    this.user = user;
    this.userId = user == null ? null : user.getId();
  }

  @JsonIgnore
  public List<Taco> getTacos() {
    return items.stream().map(OrderItem::getTaco).collect(Collectors.toList());
  }

  @JsonIgnore
  public void setTacos(List<Taco> tacos) {
    items = tacos == null ? new ArrayList<>() : tacos.stream()
        .map(taco -> new OrderItem(taco, 1, BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2)))
        .collect(Collectors.toList());
  }

  @JsonIgnore
  public void addTaco(Taco design) {
    items.add(new OrderItem(design, 1, BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2)));
  }

}
