package tacos.data;

import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import tacos.PaymentMethod;

@RepositoryRestResource(exported = false)
public interface PaymentMethodRepository extends ReactiveCrudRepository<PaymentMethod, String> {
  Mono<PaymentMethod> findByIdAndUserId(String id, String userId);
  Flux<PaymentMethod> findAllByUserIdOrderByIdDesc(String userId);
}
