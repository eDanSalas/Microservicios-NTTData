package tacos.service;

import java.time.Duration;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.IdempotencyRecord;
import tacos.IdempotencyStatus;
import tacos.TacoOrder;
import tacos.User;
import tacos.data.IdempotencyRecordRepository;
import tacos.data.OrderRepository;
import tacos.web.api.dto.OrderCreateRequest;

@Service
public class IdempotentOrderService {

  private static final Pattern KEY_PATTERN = Pattern.compile("[A-Za-z0-9._:-]+");
  private final IdempotencyRecordRepository records;
  private final OrderRepository orders;
  private final OrderCreationService orderCreation;
  private final OrderRequestHasher hasher;
  private final Duration retention;
  private final Duration inProgressRetention;
  private final Duration waitTimeout;
  private final Duration pollInterval;

  public IdempotentOrderService(IdempotencyRecordRepository records, OrderRepository orders,
      OrderCreationService orderCreation, OrderRequestHasher hasher,
      @Value("${tacocloud.idempotency.retention:24h}") Duration retention,
      @Value("${tacocloud.idempotency.in-progress-retention:10m}") Duration inProgressRetention,
      @Value("${tacocloud.idempotency.wait-timeout:5s}") Duration waitTimeout,
      @Value("${tacocloud.idempotency.poll-interval:25ms}") Duration pollInterval) {
    this.records = records;
    this.orders = orders;
    this.orderCreation = orderCreation;
    this.hasher = hasher;
    this.retention = retention;
    this.inProgressRetention = inProgressRetention;
    this.waitTimeout = waitTimeout;
    this.pollInterval = pollInterval;
  }

  public Mono<TacoOrder> create(OrderCreateRequest request, User user, String key) {
    String userId = user != null ? user.getId() : null;
    if (userId == null) return Mono.error(status(HttpStatus.UNAUTHORIZED, "Authentication is required"));
    if (!validKey(key)) return Mono.error(status(HttpStatus.BAD_REQUEST,
        "Idempotency-Key must contain 8 to 100 letters, digits, '.', '_', ':' or '-'"));
    return Mono.defer(() -> claim(request, user, userId, key));
  }

  private Mono<TacoOrder> claim(OrderCreateRequest request, User user, String userId, String key) {
    String requestHash = hasher.hash(request);
    Date now = new Date();
    IdempotencyRecord record = new IdempotencyRecord(hasher.scopedId(userId, key), userId, key,
        requestHash, UUID.randomUUID().toString(), now,
        new Date(now.getTime() + inProgressRetention.toMillis()));
    return records.save(record).onErrorResume(DuplicateKeyException.class, error -> Mono.empty())
        .flatMap(saved -> execute(saved, request, user))
        .switchIfEmpty(replay(userId, key, requestHash));
  }

  private Mono<TacoOrder> execute(IdempotencyRecord record, OrderCreateRequest request, User user) {
    return orderCreation.create(request, user, record.getOrderId())
        .flatMap(order -> complete(record).thenReturn(order))
        .onErrorResume(error -> fail(record).onErrorResume(markError -> Mono.empty())
            .then(Mono.error(error)));
  }

  private Mono<TacoOrder> replay(String userId, String key, String requestHash) {
    return records.findByUserIdAndKey(userId, key)
        .switchIfEmpty(Mono.error(status(HttpStatus.CONFLICT, "Idempotency request is unavailable")))
        .flatMap(record -> resolve(record, requestHash));
  }

  private Mono<TacoOrder> resolve(IdempotencyRecord record, String requestHash) {
    if (!record.getRequestHash().equals(requestHash)) return Mono.error(status(HttpStatus.CONFLICT,
        "Idempotency-Key was already used with a different request"));
    if (record.getStatus() == IdempotencyStatus.FAILED) return Mono.error(status(HttpStatus.CONFLICT,
        "The previous request failed; use a new Idempotency-Key"));
    return findOrder(record).switchIfEmpty(record.getStatus() == IdempotencyStatus.COMPLETED
        ? Mono.error(status(HttpStatus.CONFLICT, "The idempotent order is unavailable"))
        : waitForOrder(record));
  }

  private Mono<TacoOrder> waitForOrder(IdempotencyRecord record) {
    return Flux.interval(Duration.ZERO, pollInterval)
        .concatMap(tick -> findOrder(record).switchIfEmpty(records.findById(record.getId())
            .filter(current -> current.getStatus() == IdempotencyStatus.FAILED)
            .flatMap(current -> Mono.error(status(HttpStatus.CONFLICT,
                "The previous request failed; use a new Idempotency-Key")))))
        .next().timeout(waitTimeout, Mono.error(status(HttpStatus.CONFLICT,
            "The request with this Idempotency-Key is still in progress")));
  }

  private Mono<TacoOrder> findOrder(IdempotencyRecord record) {
    return orders.findById(record.getOrderId()).flatMap(order ->
        record.getStatus() == IdempotencyStatus.COMPLETED ? Mono.just(order)
            : complete(record).onErrorResume(error -> Mono.empty()).thenReturn(order));
  }

  private Mono<IdempotencyRecord> complete(IdempotencyRecord record) {
    return update(record, IdempotencyStatus.COMPLETED);
  }

  private Mono<IdempotencyRecord> fail(IdempotencyRecord record) {
    return update(record, IdempotencyStatus.FAILED);
  }

  private Mono<IdempotencyRecord> update(IdempotencyRecord record, IdempotencyStatus status) {
    Date now = new Date();
    record.setStatus(status);
    record.setUpdatedAt(now);
    record.setExpiresAt(new Date(now.getTime() + retention.toMillis()));
    return records.save(record);
  }

  private boolean validKey(String key) {
    return key != null && key.length() >= 8 && key.length() <= 100
        && KEY_PATTERN.matcher(key).matches();
  }

  private ResponseStatusException status(HttpStatus status, String reason) {
    return new ResponseStatusException(status, reason);
  }
}
