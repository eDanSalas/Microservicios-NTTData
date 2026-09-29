package tacos.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.AnnouncementSeverity;
import tacos.OpsAnnouncement;
import tacos.User;
import tacos.data.OpsAnnouncementRepository;

@Service
public class OpsAnnouncementService {
  private final OpsAnnouncementRepository repo;
  private final Clock clock;
  private final int maxTextLength;
  private final int maxActive;
  private final Duration maxTtl;

  public OpsAnnouncementService(OpsAnnouncementRepository repo,
      @Qualifier("announcementClock") Clock clock,
      @Value("${tacocloud.announcements.max-text-length:500}") int maxTextLength,
      @Value("${tacocloud.announcements.max-active:20}") int maxActive,
      @Value("${tacocloud.announcements.max-ttl:30d}") Duration maxTtl) {
    this.repo = repo;
    this.clock = clock;
    this.maxTextLength = maxTextLength;
    this.maxActive = maxActive;
    this.maxTtl = maxTtl;
  }

  public Flux<OpsAnnouncement> findActive() {
    return repo.findByActiveTrueAndExpiresAtAfterOrderByCreatedAtDesc(clock.instant());
  }

  public Mono<OpsAnnouncement> create(String text, AnnouncementSeverity severity,
      Instant expiresAt, User actor) {
    Instant now = clock.instant();
    String validText = validateText(text);
    validateExpiration(now, expiresAt);
    if (severity == null) return Mono.error(invalid("Severity is required"));
    OpsAnnouncement announcement = new OpsAnnouncement(UUID.randomUUID().toString(), validText,
        severity, now, expiresAt, userId(actor));
    return repo.countByActiveTrueAndExpiresAtAfter(now).flatMap(count -> count >= maxActive
        ? Mono.error(new ResponseStatusException(HttpStatus.CONFLICT,
            "Active announcement limit reached")) : repo.save(announcement));
  }

  public Mono<Void> delete(String id) {
    if (id == null || id.isBlank()) return Mono.error(invalid("Announcement id is required"));
    return repo.findByIdAndActiveTrue(id).switchIfEmpty(Mono.error(new ResponseStatusException(
        HttpStatus.NOT_FOUND, "Announcement not found"))).flatMap(announcement -> {
          announcement.setActive(false);
          return repo.save(announcement);
        }).then();
  }

  @Scheduled(fixedDelayString = "${tacocloud.announcements.cleanup-delay-ms:60000}")
  public void cleanupExpired() {
    repo.deleteByExpiresAtLessThanEqual(clock.instant()).subscribe();
  }

  private String validateText(String text) {
    String value = text == null ? "" : text.trim();
    if (value.isEmpty() || value.length() > maxTextLength
        || value.chars().anyMatch(Character::isISOControl))
      throw invalid("Announcement text is invalid");
    return value;
  }

  private void validateExpiration(Instant now, Instant expiresAt) {
    if (expiresAt == null || !expiresAt.isAfter(now) || expiresAt.isAfter(now.plus(maxTtl)))
      throw invalid("Expiration is outside the allowed range");
  }

  private String userId(User user) {
    if (user == null || user.getId() == null || user.getId().isBlank())
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
    return user.getId();
  }

  private ResponseStatusException invalid(String reason) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
  }
}
