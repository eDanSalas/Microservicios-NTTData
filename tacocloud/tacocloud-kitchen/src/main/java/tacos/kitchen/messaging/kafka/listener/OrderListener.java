package tacos.kitchen.messaging.kafka.listener;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import tacos.kitchen.KitchenUI;
import tacos.kitchen.KitchenMetrics;
import tacos.messaging.OrderEvent;

@Profile("kafka-listener")
@Component
@Slf4j
public class OrderListener {
  
  private KitchenUI ui;
  private final KitchenMetrics metrics;

  @Autowired
  public OrderListener(KitchenUI ui, KitchenMetrics metrics) {
    this.ui = ui;
    this.metrics = metrics;
  }

  @KafkaListener(topics = "${tacocloud.messaging.kafka.topic}")
  public void handle(OrderEvent event, ConsumerRecord<String, OrderEvent> record) {
    log.error("Received from partition {} with timestamp {}",
        record.partition(), record.timestamp());
    
    metrics.received(event, "kafka");
    ui.displayOrder(event);
  }
  
}
