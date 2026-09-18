package tacos.data.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.mongodb.client.result.UpdateResult;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.mongodb.core
    .ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query
    .UpdateDefinition;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.PaymentMethod;
import tacos.TacoOrder;

import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;

public class SensitivePaymentDataMigrationTest {

  @Test
  public void removesLegacyFieldsFromBothCollections() {
    ReactiveMongoTemplate template =
        Mockito.mock(
            ReactiveMongoTemplate.class);

    when(template.updateMulti(
            any(Query.class),
            any(UpdateDefinition.class),
            eq(TacoOrder.class)))
        .thenReturn(
            Mono.just(
                UpdateResult.acknowledged(
                    3L,
                    2L,
                    null)));

    when(template.updateMulti(
            any(Query.class),
            any(UpdateDefinition.class),
            eq(PaymentMethod.class)))
        .thenReturn(
            Mono.just(
                UpdateResult.acknowledged(
                    2,
                    2L,
                    null)));

    SensitivePaymentDataMigration migration =
        new SensitivePaymentDataMigration(
            template);

    StepVerifier.create(migration.migrate())
        .assertNext(result -> {
          assertEquals(
              2,
              result.getModifiedOrders());

          assertEquals(
              2,
              result.getModifiedPaymentMethods());
        })
        .verifyComplete();

    ArgumentCaptor<UpdateDefinition> update =
        ArgumentCaptor.forClass(
            UpdateDefinition.class);

    Mockito.verify(template)
        .updateMulti(
            any(),
            update.capture(),
            eq(TacoOrder.class));

    Document unset =
        update.getValue()
            .getUpdateObject()
            .get(
                "$unset",
                Document.class);

    assertTrue(unset.containsKey("ccNumber"));
    assertTrue(unset.containsKey("ccExpiration"));
    assertTrue(unset.containsKey("ccCVV"));
  }
}