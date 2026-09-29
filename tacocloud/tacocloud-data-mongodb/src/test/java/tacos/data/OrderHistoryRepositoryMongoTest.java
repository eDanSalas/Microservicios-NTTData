package tacos.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

import tacos.OrderStatus;
import tacos.TacoOrder;
import tacos.User;

@DataMongoTest(properties = {"spring.mongodb.embedded.version=4.0.12",
    "spring.data.mongodb.auto-index-creation=true"})
@EnabledIfSystemProperty(named = "tacocloud.mongo.integration", matches = "true")
public class OrderHistoryRepositoryMongoTest {
  @SpringBootConfiguration
  @EnableAutoConfiguration
  static class TestApplication {
  }

  @Autowired
  private OrderRepository repository;

  @Autowired
  private ReactiveMongoTemplate template;

  @BeforeEach
  public void clean() {
    template.remove(TacoOrder.class).all().block();
  }

  @Test
  public void shouldIsolateOrdersByOwner() {
    template.insertAll(List.of(order("a-1", user("a"), 1), order("b-1", user("b"), 2)))
        .then().block();

    List<String> ids = repository.findByUserId("a", page(0, 10)).map(TacoOrder::getId)
        .collectList().block();

    assertEquals(List.of("a-1"), ids);
    assertEquals(1, repository.countByUserId("a").block());
    assertEquals("a-1", repository.findByIdAndUserId("a-1", "a").block().getId());
    assertEquals(null, repository.findByIdAndUserId("b-1", "a").block());
  }

  @Test
  public void shouldPageByDateAndStableDescendingId() {
    User owner = user("a");
    template.insertAll(List.of(order("a-1", owner, 1), order("a-2", owner, 2),
        order("a-3", owner, 2))).then().block();

    assertEquals(List.of("a-3", "a-2"), repository.findByUserId("a", page(0, 2))
        .map(TacoOrder::getId).collectList().block());
    assertEquals(List.of("a-1"), repository.findByUserId("a", page(1, 2))
        .map(TacoOrder::getId).collectList().block());
  }

  private PageRequest page(int page, int size) {
    return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "placedAt")
        .and(Sort.by(Sort.Direction.DESC, "id")));
  }

  private TacoOrder order(String id, User user, long time) {
    TacoOrder order = new TacoOrder();
    order.setId(id);
    order.setUser(user);
    order.setPlacedAt(new Date(time));
    order.setStatus(OrderStatus.PLACED);
    return order;
  }

  private User user(String id) {
    User user = new User(id, "password", id, "street", "city", "state", "zip", "phone",
        id + "@example.com");
    user.setId(id);
    return user;
  }
}
