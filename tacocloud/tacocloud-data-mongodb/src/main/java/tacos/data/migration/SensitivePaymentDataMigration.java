package tacos.data.migration;

import com.mongodb.client.result.UpdateResult;

import lombok.Value;

import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;
import tacos.PaymentMethod;
import tacos.TacoOrder;

@Component
public class SensitivePaymentDataMigration {

  private final ReactiveMongoTemplate mongoTemplate;

  public SensitivePaymentDataMigration(ReactiveMongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  public Mono<MigrationResult> migrate() {
    Mono<UpdateResult> orders =
        mongoTemplate.updateMulti(
            sensitiveFieldsQuery(),
            sensitiveFieldsUpdate(),
            TacoOrder.class);

    Mono<UpdateResult> paymentMethods = mongoTemplate.updateMulti(
            sensitiveFieldsQuery(),
            sensitiveFieldsUpdate(),
            PaymentMethod.class);

    return Mono.zip(
            orders,
            paymentMethods)
        .map(results ->
            new MigrationResult(
                results.getT1().getMatchedCount(),
                results.getT1().getModifiedCount(),
                results.getT2().getMatchedCount(),
                results.getT2().getModifiedCount()));
  }

  private Query sensitiveFieldsQuery() {
    return Query.query(
        new Criteria().orOperator(
            Criteria.where("ccNumber")
                .exists(true),
            Criteria.where("ccExpiration")
                .exists(true),
            Criteria.where("ccCVV")
                .exists(true)));
  }

  private Update sensitiveFieldsUpdate() {
    return new Update()
        .unset("ccNumber")
        .unset("ccExpiration")
        .unset("ccCVV");
  }

  @Value
  public static class MigrationResult {
    long matchedOrders;
    long modifiedOrders;
    long matchedPaymentMethods;
    long modifiedPaymentMethods;
  }
}