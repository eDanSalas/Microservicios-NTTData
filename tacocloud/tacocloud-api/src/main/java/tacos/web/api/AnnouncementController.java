package tacos.web.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import tacos.OpsAnnouncement;
import tacos.service.OpsAnnouncementService;
import tacos.web.api.dto.AnnouncementResponse;

@RestController
@RequestMapping(path = "/api/announcements", produces = "application/json")
public class AnnouncementController {
  private final OpsAnnouncementService service;

  public AnnouncementController(OpsAnnouncementService service) {
    this.service = service;
  }

  @GetMapping
  public Flux<AnnouncementResponse> findActive() {
    return service.findActive().map(this::response);
  }

  private AnnouncementResponse response(OpsAnnouncement announcement) {
    return new AnnouncementResponse(announcement.getId(), announcement.getText(),
        announcement.getSeverity(), announcement.getCreatedAt(), announcement.getExpiresAt());
  }
}
