package tacos;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.Set;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.convert
    .MappingMongoConverter;
import org.springframework.data.mongodb.core.convert
    .NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping
    .MongoMappingContext;

import com.fasterxml.jackson.databind.ObjectMapper;

public class PaymentDataContractTest {

  private MappingMongoConverter converter;
  private ObjectMapper objectMapper;

  @BeforeEach
  public void setUp() throws Exception {
    MongoMappingContext context =
        new MongoMappingContext();

    context.setInitialEntitySet(
        Set.of(
            TacoOrder.class,
            PaymentMethod.class));

    context.afterPropertiesSet();

    converter =
        new MappingMongoConverter(
            NoOpDbRefResolver.INSTANCE,
            context);

    converter.afterPropertiesSet();

    objectMapper =
        new ObjectMapper()
            .findAndRegisterModules();
  }

  @Test
  public void orderPersistenceContainsOnlySafeReference()
      throws Exception {

    TacoOrder order = new TacoOrder();

    order.setPaymentMethodId("pm-1");
    order.setPaymentBrand("VISA");
    order.setPaymentLast4("1111");

    Document document = new Document();
    converter.write(order, document);

    assertTrue(
        document.containsKey(
            "paymentMethodId"));

    assertFalse(document.containsKey("ccNumber"));
    assertFalse(document.containsKey("ccExpiration"));
    assertFalse(document.containsKey("ccCVV"));

    String eventJson =
        objectMapper.writeValueAsString(order);

    assertFalse(
        eventJson.contains(
            "paymentMethodId"));

    assertFalse(
        eventJson.contains(
            "paymentBrand"));

    assertFalse(
        eventJson.contains(
            "paymentLast4"));
  }

  @Test
  public void paymentMethodPersistenceContainsNoPanOrCvv() {
    PaymentMethod method =
        new PaymentMethod(
            "pm-1",
            "user-1",
            "tok_fake_abc",
            "VISA",
            "1111",
            12,
            2099,
            new Date());

    Document document = new Document();
    converter.write(method, document);

    assertTrue(
        document.containsKey(
            "paymentToken"));

    assertFalse(document.containsKey("ccNumber"));
    assertFalse(document.containsKey("ccExpiration"));
    assertFalse(document.containsKey("ccCVV"));

    assertFalse(
        method.toString()
            .contains("tok_fake_abc"));
  }
}