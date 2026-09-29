package tacos.messaging;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NoOpOrderMessagingService
       implements OrderMessagingService {
  
  @Override
  public void sendOrder(OrderEvent event) {
    log.info("Sending order event {} for order {}", event.getEventId(),
        event.getPayload().getOrderId());
  }
  
}
