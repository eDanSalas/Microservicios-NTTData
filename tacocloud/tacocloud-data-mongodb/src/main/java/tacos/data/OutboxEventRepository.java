package tacos.data;

import java.util.Collection;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Mono;

public interface OutboxEventRepository extends ReactiveCrudRepository<OutboxEvent, String>,
    OutboxEventRepositoryCustom {
  Mono<Long> countByStatusIn(Collection<OutboxStatus> statuses);
}
