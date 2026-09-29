package tacos.messaging;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

public class RabbitOrderMessagingService
       implements OrderMessagingService {
  
  private final RabbitTemplate rabbit;
  private final String exchange;
  private final String routingKey;

  public RabbitOrderMessagingService(RabbitTemplate rabbit, String exchange,
      String routingKey) {
    this.rabbit = rabbit;
    this.exchange = exchange;
    this.routingKey = routingKey;
  }
  
  public void sendOrder(OrderEvent event) {
    rabbit.convertAndSend(exchange, routingKey, event,
        new MessagePostProcessor() {
          @Override
          public Message postProcessMessage(Message message)
              throws AmqpException {
            MessageProperties props = message.getMessageProperties();
            props.setHeader("X_ORDER_SOURCE", "WEB");
            props.setHeader("X_CORRELATION_ID", event.getCorrelationId());
            return message;
          } 
        });
  }
  
}
