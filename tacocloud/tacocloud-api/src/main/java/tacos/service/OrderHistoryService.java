package tacos.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.OrderStatus;
import tacos.TacoOrder;
import tacos.User;
import tacos.data.OrderRepository;

@Service
public class OrderHistoryService {
  private final OrderRepository repo;
  private final int maximumSize;

  public OrderHistoryService(OrderRepository repo,
      @Value("${tacocloud.order.history-max-size:50}") int maximumSize) {
    this.repo = repo;
    this.maximumSize = maximumSize;
  }

  public Mono<OrderHistoryPage> findMine(User user, int page, int size) {
    String userId = userId(user);
    Pageable pageable = pageable(page, size);
    return result(repo.findByUserId(userId, pageable), repo.countByUserId(userId), page, size);
  }

  public Mono<TacoOrder> findMine(User user, String orderId) {
    return repo.findByIdAndUserId(orderId, userId(user)).switchIfEmpty(Mono.error(
        new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found")));
  }

  public Mono<OrderHistoryPage> findAll(String userId, OrderStatus status, int page, int size) {
    Pageable pageable = pageable(page, size);
    if (userId != null && !userId.isBlank() && status != null)
      return result(repo.findByUserIdAndStatus(userId, status, pageable),
          repo.countByUserIdAndStatus(userId, status), page, size);
    if (userId != null && !userId.isBlank()) return result(repo.findByUserId(userId, pageable),
        repo.countByUserId(userId), page, size);
    if (status != null) return result(repo.findByStatus(status, pageable), repo.countByStatus(status),
        page, size);
    return result(repo.findAllBy(pageable), repo.count(), page, size);
  }

  private Mono<OrderHistoryPage> result(Flux<TacoOrder> content, Mono<Long> count, int page,
      int size) {
    return content.collectList().zipWith(count).map(result -> new OrderHistoryPage(result.getT1(),
        page, size, result.getT2(), (int) ((result.getT2() + size - 1) / size)));
  }

  private Pageable pageable(int page, int size) {
    if (page < 0 || size < 1 || size > maximumSize) throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "Page must be non-negative and size between 1 and " + maximumSize);
    return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "placedAt")
        .and(Sort.by(Sort.Direction.DESC, "id")));
  }

  private String userId(User user) {
    if (user == null || user.getId() == null || user.getId().isBlank()) throw new ResponseStatusException(
        HttpStatus.UNAUTHORIZED, "Authentication is required");
    return user.getId();
  }
}
