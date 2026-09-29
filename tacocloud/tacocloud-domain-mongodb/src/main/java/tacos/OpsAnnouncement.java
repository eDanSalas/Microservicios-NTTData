package tacos;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Document("ops_announcements")
public class OpsAnnouncement {
  @Id
  private String id;
  private String text;
  private AnnouncementSeverity severity;
  private Instant createdAt;
  @Indexed(expireAfterSeconds = 0)
  private Instant expiresAt;
  private String createdBy;
  private boolean active;

  public OpsAnnouncement(String id, String text, AnnouncementSeverity severity, Instant createdAt,
      Instant expiresAt, String createdBy) {
    this.id = id;
    this.text = text;
    this.severity = severity;
    this.createdAt = createdAt;
    this.expiresAt = expiresAt;
    this.createdBy = createdBy;
    this.active = true;
  }
}
