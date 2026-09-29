package tacos.web.api.dto;

import java.time.Instant;

import lombok.Value;
import tacos.AnnouncementSeverity;

@Value
public class AnnouncementResponse {
  String id;
  String text;
  AnnouncementSeverity severity;
  Instant createdAt;
  Instant expiresAt;
}
