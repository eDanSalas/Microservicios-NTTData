package tacos.web.api;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;
import tacos.OpsAnnouncement;
import tacos.User;
import tacos.service.OpsAnnouncementService;
import tacos.web.api.dto.AnnouncementRequest;
import tacos.web.api.dto.AnnouncementResponse;

@RestController
@RequestMapping(path = "/api/admin/announcements", produces = "application/json")
public class AdminAnnouncementController {
  private final OpsAnnouncementService service;

  public AdminAnnouncementController(OpsAnnouncementService service) {
    this.service = service;
  }

  @PostMapping(consumes = "application/json")
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<AnnouncementResponse> create(@RequestBody AnnouncementRequest request,
      @AuthenticationPrincipal User actor) {
    return service.create(request.getText(), request.getSeverity(), request.getExpiresAt(), actor)
        .map(this::response);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Mono<Void> delete(@PathVariable String id) {
    return service.delete(id);
  }

  private AnnouncementResponse response(OpsAnnouncement announcement) {
    return new AnnouncementResponse(announcement.getId(), announcement.getText(),
        announcement.getSeverity(), announcement.getCreatedAt(), announcement.getExpiresAt());
  }
}
