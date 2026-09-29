package tacos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import tacos.AnnouncementSeverity;
import tacos.OpsAnnouncement;
import tacos.User;
import tacos.data.OpsAnnouncementRepository;

public class OpsAnnouncementServiceTest {
  private final Instant now = Instant.parse("2026-09-21T12:00:00Z");
  private final OpsAnnouncementRepository repo = mock(OpsAnnouncementRepository.class);
  private final User admin = admin();
  private OpsAnnouncementService service;

  @BeforeEach
  public void setUp() {
    service = new OpsAnnouncementService(repo, Clock.fixed(now, ZoneOffset.UTC), 40, 2,
        Duration.ofDays(7));
  }

  @Test
  public void shouldPersistAndLoadAfterServiceRestart() {
    when(repo.countByActiveTrueAndExpiresAtAfter(now)).thenReturn(Mono.just(0L));
    when(repo.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    OpsAnnouncement saved = service.create(" Maintenance tonight ",
        AnnouncementSeverity.WARNING, now.plus(Duration.ofHours(2)), admin).block();
    when(repo.findByActiveTrueAndExpiresAtAfterOrderByCreatedAtDesc(now))
        .thenReturn(Flux.just(saved));
    OpsAnnouncementService restarted = new OpsAnnouncementService(repo,
        Clock.fixed(now, ZoneOffset.UTC), 40, 2, Duration.ofDays(7));

    StepVerifier.create(restarted.findActive()).assertNext(announcement -> {
      UUID.fromString(announcement.getId());
      assertEquals("Maintenance tonight", announcement.getText());
      assertEquals("admin-1", announcement.getCreatedBy());
      assertTrue(announcement.isActive());
    }).verifyComplete();
  }

  @Test
  public void shouldKeepConcurrentWritesIndependent() {
    when(repo.countByActiveTrueAndExpiresAtAfter(now)).thenReturn(Mono.just(0L));
    when(repo.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(Flux.merge(
        service.create("First", AnnouncementSeverity.INFO, now.plusSeconds(60), admin)
            .subscribeOn(Schedulers.parallel()),
        service.create("Second", AnnouncementSeverity.CRITICAL, now.plusSeconds(60), admin)
            .subscribeOn(Schedulers.parallel())).collectList()).assertNext(values -> {
              assertEquals(2, values.size());
              assertFalse(values.get(0).getId().equals(values.get(1).getId()));
            }).verifyComplete();
  }

  @Test
  public void shouldFilterAndCleanExpiredUsingClock() {
    when(repo.findByActiveTrueAndExpiresAtAfterOrderByCreatedAtDesc(now)).thenReturn(Flux.empty());
    when(repo.deleteByExpiresAtLessThanEqual(now)).thenReturn(Mono.just(2L));

    StepVerifier.create(service.findActive()).verifyComplete();
    service.cleanupExpired();

    verify(repo).findByActiveTrueAndExpiresAtAfterOrderByCreatedAtDesc(now);
    verify(repo).deleteByExpiresAtLessThanEqual(now);
  }

  @Test
  public void shouldDeleteTheRequestedStableId() {
    OpsAnnouncement announcement = new OpsAnnouncement("announcement-2", "Text",
        AnnouncementSeverity.INFO, now, now.plusSeconds(60), "admin-1");
    when(repo.findByIdAndActiveTrue("announcement-2")).thenReturn(Mono.just(announcement));
    when(repo.save(announcement)).thenReturn(Mono.just(announcement));

    StepVerifier.create(service.delete("announcement-2")).verifyComplete();

    assertFalse(announcement.isActive());
    verify(repo).findByIdAndActiveTrue("announcement-2");
  }

  @Test
  public void shouldRejectInvalidTextExpirationAndLimit() {
    assertStatus(HttpStatus.BAD_REQUEST, () -> service.create("bad\ntext",
        AnnouncementSeverity.INFO, now.plusSeconds(60), admin));
    assertStatus(HttpStatus.BAD_REQUEST, () -> service.create("valid",
        AnnouncementSeverity.INFO, now.plus(Duration.ofDays(8)), admin));
    when(repo.countByActiveTrueAndExpiresAtAfter(now)).thenReturn(Mono.just(2L));
    StepVerifier.create(service.create("valid", AnnouncementSeverity.INFO,
        now.plusSeconds(60), admin)).expectErrorSatisfies(error -> assertEquals(
            HttpStatus.CONFLICT, ((ResponseStatusException) error).getStatus())).verify();
  }

  private void assertStatus(HttpStatus status, Runnable action) {
    ResponseStatusException error = assertThrows(ResponseStatusException.class, action::run);
    assertEquals(status, error.getStatus());
  }

  private User admin() {
    User user = new User("admin", "password", "Admin", "Street", "City", "State", "00000",
        "0000000000", "admin@example.com");
    user.setId("admin-1");
    return user;
  }
}
