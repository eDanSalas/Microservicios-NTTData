package tacos.kitchen.processing;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface KitchenOrderStateRepository extends MongoRepository<KitchenOrderState, String> {
}
