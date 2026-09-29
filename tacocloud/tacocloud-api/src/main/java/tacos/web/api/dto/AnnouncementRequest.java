package tacos.web.api.dto;

import java.time.Instant;

import lombok.Data;
import tacos.AnnouncementSeverity;

@Data
public class AnnouncementRequest {
  private String text;
  private AnnouncementSeverity severity;
  private Instant expiresAt;
}
