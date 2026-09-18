package tacos;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Document
public class InventoryReservation {

  @Id
  private String id;
  private String orderId;
  private String idempotencyKey;
  private InventoryReservationStatus status = InventoryReservationStatus.PENDING;
  private List<InventoryReservationLine> lines = new ArrayList<>();

  public InventoryReservation(String id, String orderId, String idempotencyKey,
      List<InventoryReservationLine> lines) {
    this.id = id;
    this.orderId = orderId;
    this.idempotencyKey = idempotencyKey;
    this.lines = lines;
  }
}
