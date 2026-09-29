package tacos.data;

import java.time.Instant;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.OpsAnnouncement;

public interface OpsAnnouncementRepository extends ReactiveCrudRepository<OpsAnnouncement, String> {
  Flux<OpsAnnouncement> findByActiveTrueAndExpiresAtAfterOrderByCreatedAtDesc(Instant now);
  Mono<Long> countByActiveTrueAndExpiresAtAfter(Instant now);
  Mono<OpsAnnouncement> findByIdAndActiveTrue(String id);
  Mono<Long> deleteByExpiresAtLessThanEqual(Instant now);
}
