package tacos.kitchen.messaging.rabbit;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import tacos.kitchen.OrderReceiver;
import tacos.messaging.OrderEvent;

@Profile("rabbitmq-template")
@Component("templateOrderReceiver")
public class RabbitOrderReceiver implements OrderReceiver {

  private final RabbitTemplate rabbit;
  private final String queue;

  public RabbitOrderReceiver(RabbitTemplate rabbit,
      @Value("${tacocloud.messaging.rabbit.queue}") String queue) {
    this.rabbit = rabbit;
    this.queue = queue;
  }
  
  public OrderEvent receiveOrder() {
    return (OrderEvent) rabbit.receiveAndConvert(queue);
  }
  
}
