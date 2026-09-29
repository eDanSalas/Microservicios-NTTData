package tacos;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Document("idempotency_records")
@CompoundIndex(name = "uk_idempotency_user_key", def = "{'userId': 1, 'key': 1}", unique = true)
public class IdempotencyRecord {

  @Id
  private String id;
  @Version
  private Long version;
  private String userId;
  private String key;
  private String requestHash;
  private String orderId;
  private IdempotencyStatus status;
  private Date createdAt;
  private Date updatedAt;
  @Indexed(name = "ttl_idempotency", expireAfterSeconds = 0)
  private Date expiresAt;

  public IdempotencyRecord(String id, String userId, String key, String requestHash,
      String orderId, Date now, Date expiresAt) {
    this.id = id;
    this.userId = userId;
    this.key = key;
    this.requestHash = requestHash;
    this.orderId = orderId;
    this.status = IdempotencyStatus.IN_PROGRESS;
    this.createdAt = now;
    this.updatedAt = now;
    this.expiresAt = expiresAt;
  }
}
