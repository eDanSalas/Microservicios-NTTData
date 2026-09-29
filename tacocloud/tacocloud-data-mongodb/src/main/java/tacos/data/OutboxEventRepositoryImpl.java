package tacos.data;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class OutboxEventRepositoryImpl implements OutboxEventRepositoryCustom {

  private final ReactiveMongoTemplate mongo;

  public OutboxEventRepositoryImpl(ReactiveMongoTemplate mongo) {
    this.mongo = mongo;
  }

  @Override
  public Flux<OutboxEvent> claimBatch(Instant now, int batchSize, int maxAttempts,
      Duration claimTimeout) {
    return expireExhaustedClaims(now, maxAttempts, claimTimeout)
        .thenMany(Flux.range(0, batchSize)
            .concatMap(ignored -> claim(now, maxAttempts, claimTimeout)));
  }

  @Override
  public Mono<OutboxEvent> markPublished(OutboxEvent event, Instant now) {
    Query query = claimedEvent(event);
    Update update = new Update().set("status", OutboxStatus.PUBLISHED)
        .set("publishedAt", now).set("updatedAt", now).unset("claimId")
        .unset("claimedAt").unset("nextAttemptAt").unset("lastError");
    return mongo.findAndModify(query, update, options(), OutboxEvent.class);
  }

  @Override
  public Mono<OutboxEvent> markFailed(OutboxEvent event, Instant now, Instant nextAttemptAt,
      String error) {
    Query query = claimedEvent(event);
    Update update = new Update().set("status", OutboxStatus.FAILED)
        .set("nextAttemptAt", nextAttemptAt).set("lastError", error)
        .set("updatedAt", now).unset("claimId").unset("claimedAt");
    return mongo.findAndModify(query, update, options(), OutboxEvent.class);
  }

  private Mono<OutboxEvent> claim(Instant now, int maxAttempts, Duration claimTimeout) {
    Criteria due = new Criteria().orOperator(Criteria.where("nextAttemptAt").lte(now),
        Criteria.where("nextAttemptAt").exists(false), Criteria.where("nextAttemptAt").is(null));
    Criteria ready = new Criteria().andOperator(
        Criteria.where("status").in(OutboxStatus.NEW, OutboxStatus.FAILED),
        Criteria.where("attempts").lt(maxAttempts), due);
    Criteria stale = new Criteria().andOperator(
        Criteria.where("status").is(OutboxStatus.PUBLISHING),
        Criteria.where("attempts").lt(maxAttempts),
        Criteria.where("claimedAt").lte(now.minus(claimTimeout)));
    Query query = new Query(new Criteria().orOperator(ready, stale))
        .with(Sort.by(Sort.Direction.ASC, "createdAt"));
    Update update = new Update().set("status", OutboxStatus.PUBLISHING)
        .set("claimId", UUID.randomUUID().toString()).set("claimedAt", now)
        .set("updatedAt", now).inc("attempts", 1).unset("lastError");
    return mongo.findAndModify(query, update, options(), OutboxEvent.class);
  }

  private Mono<Void> expireExhaustedClaims(Instant now, int maxAttempts,
      Duration claimTimeout) {
    Query query = Query.query(Criteria.where("status").is(OutboxStatus.PUBLISHING)
        .and("attempts").gte(maxAttempts).and("claimedAt").lte(now.minus(claimTimeout)));
    Update update = new Update().set("status", OutboxStatus.FAILED).set("updatedAt", now)
        .set("lastError", "Claim expired after maximum attempts")
        .unset("claimId").unset("claimedAt");
    return mongo.updateMulti(query, update, OutboxEvent.class).then();
  }

  private Query claimedEvent(OutboxEvent event) {
    return Query.query(Criteria.where("_id").is(event.getEventId())
        .and("claimId").is(event.getClaimId()).and("status").is(OutboxStatus.PUBLISHING));
  }

  private FindAndModifyOptions options() {
    return FindAndModifyOptions.options().returnNew(true);
  }
}
