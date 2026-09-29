package tacos.web.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import tacos.AnnouncementSeverity;
import tacos.web.api.dto.AnnouncementResponse;

public class AnnouncementResponseTest {
  @Test
  public void shouldExposeOnlyPublicAnnouncementFields() throws Exception {
    String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(
        new AnnouncementResponse("id-1", "Maintenance", AnnouncementSeverity.WARNING,
            Instant.parse("2026-09-21T12:00:00Z"), Instant.parse("2026-09-21T14:00:00Z")));

    assertTrue(json.contains("Maintenance"));
    assertFalse(json.contains("createdBy"));
    assertFalse(json.contains("active"));
  }
}
