package tacos.kitchen.messaging.rabbit.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import tacos.kitchen.KitchenMetrics;
import tacos.kitchen.KitchenUI;
import tacos.kitchen.processing.KitchenEventProcessor;
import tacos.kitchen.processing.ProcessingResult;
import tacos.messaging.OrderEvent;

@Profile("rabbitmq-listener")
@Component
public class OrderListener {
  
  private final KitchenUI ui;
  private final KitchenEventProcessor processor;
  private final KitchenMetrics metrics;

  public OrderListener(KitchenUI ui, KitchenEventProcessor processor, KitchenMetrics metrics) {
    this.ui = ui;
    this.processor = processor;
    this.metrics = metrics;
  }

  @RabbitListener(queues = "${tacocloud.messaging.rabbit.queue}")
  public void receiveOrder(OrderEvent event) {
    ProcessingResult result = processor.process(event);
    metrics.received(event, result, "rabbit");
    if (result == ProcessingResult.PROCESSED) ui.displayOrder(event);
  }
  
}
