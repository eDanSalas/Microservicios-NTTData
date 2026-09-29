package tacos.data;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Mono;
import tacos.IdempotencyRecord;

public interface IdempotencyRecordRepository
    extends ReactiveCrudRepository<IdempotencyRecord, String> {

  Mono<IdempotencyRecord> findByUserIdAndKey(String userId, String key);
}
