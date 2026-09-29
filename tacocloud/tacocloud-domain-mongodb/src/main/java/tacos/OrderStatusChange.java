package tacos;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusChange {
  private OrderStatus fromStatus;
  private OrderStatus toStatus;
  private Date changedAt;
  private String actorId;
  private String actorRole;
  private String origin;
  private String reason;
}
