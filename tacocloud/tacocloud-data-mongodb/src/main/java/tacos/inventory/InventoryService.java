package tacos.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.Ingredient;
import tacos.InventoryReservation;
import tacos.InventoryReservationLine;
import tacos.InventoryReservationStatus;
import tacos.OrderItem;
import tacos.TacoOrder;

@Service
public class InventoryService {

  private final ReactiveMongoTemplate template;

  public InventoryService(ReactiveMongoTemplate template) {
    this.template = template;
  }

  public Mono<InventoryReservation> reserve(TacoOrder order, String idempotencyKey) {
    if (order == null || order.getId() == null || idempotencyKey == null || idempotencyKey.isBlank())
      return Mono.error(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid inventory reservation"));
    InventoryReservation reservation = new InventoryReservation(idempotencyKey, order.getId(), idempotencyKey,
        reservationLines(order));
    return template.findById(idempotencyKey, InventoryReservation.class)
        .switchIfEmpty(Mono.defer(() -> template.insert(reservation).flatMap(this::reserveNew)
            .onErrorResume(DuplicateKeyException.class,
                error -> template.findById(idempotencyKey, InventoryReservation.class))));
  }

  public Mono<InventoryReservation> release(String idempotencyKey) {
    Query query = Query.query(Criteria.where("_id").is(idempotencyKey).and("status")
        .is(InventoryReservationStatus.RESERVED));
    Update update = Update.update("status", InventoryReservationStatus.RELEASING);
    return template.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true),
        InventoryReservation.class).flatMap(this::releaseClaimed)
        .switchIfEmpty(template.findById(idempotencyKey, InventoryReservation.class));
  }

  private Mono<InventoryReservation> reserveNew(InventoryReservation reservation) {
    List<InventoryReservationLine> reserved = new ArrayList<>();
    return Flux.fromIterable(reservation.getLines()).concatMap(line -> reserveLine(line)
        .doOnNext(ingredient -> reserved.add(line))).then(mark(reservation, InventoryReservationStatus.RESERVED))
        .onErrorResume(error -> compensate(reserved)
            .then(mark(reservation, InventoryReservationStatus.COMPENSATED)).then(Mono.error(error)));
  }

  private Mono<Ingredient> reserveLine(InventoryReservationLine line) {
    Query query = Query.query(Criteria.where("_id").is(line.getIngredientId()).and("available").is(true)
        .and("stockOnHand").gte(line.getQuantity()));
    Update update = new Update().inc("stockOnHand", -line.getQuantity());
    return template.findAndModify(query, update, FindAndModifyOptions.options().returnNew(true), Ingredient.class)
        .switchIfEmpty(Mono.error(new InsufficientStockException(line.getIngredientId())))
        .flatMap(ingredient -> ingredient.getStockOnHand() == 0
            ? template.updateFirst(Query.query(Criteria.where("_id").is(ingredient.getId()).and("stockOnHand").is(0)),
                Update.update("available", false), Ingredient.class).thenReturn(ingredient)
            : Mono.just(ingredient));
  }

  private Mono<InventoryReservation> releaseClaimed(InventoryReservation reservation) {
    return restore(reservation.getLines()).then(mark(reservation, InventoryReservationStatus.RELEASED));
  }

  private Mono<Void> compensate(List<InventoryReservationLine> lines) {
    return restore(lines);
  }

  private Mono<Void> restore(List<InventoryReservationLine> lines) {
    return Flux.fromIterable(lines).concatMap(line -> template.updateFirst(
        Query.query(Criteria.where("_id").is(line.getIngredientId())),
        new Update().inc("stockOnHand", line.getQuantity()).set("available", true), Ingredient.class)).then();
  }

  private Mono<InventoryReservation> mark(InventoryReservation reservation, InventoryReservationStatus status) {
    reservation.setStatus(status);
    return template.save(reservation);
  }

  private List<InventoryReservationLine> reservationLines(TacoOrder order) {
    Map<String, Integer> quantities = new TreeMap<>();
    for (OrderItem item : order.getItems()) {
      for (Ingredient ingredient : item.getTaco().getIngredients()) {
        try {
          quantities.merge(ingredient.getId(), item.getQuantity(), Math::addExact);
        } catch (ArithmeticException exception) {
          throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Inventory quantity is too large");
        }
      }
    }
    if (quantities.isEmpty())
      throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Order has no inventory to reserve");
    return quantities.entrySet().stream().map(entry -> new InventoryReservationLine(entry.getKey(), entry.getValue()))
        .toList();
  }
}
