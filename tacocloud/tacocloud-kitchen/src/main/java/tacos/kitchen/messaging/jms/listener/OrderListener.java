package tacos.kitchen.messaging.jms.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import tacos.kitchen.KitchenUI;
import tacos.kitchen.KitchenMetrics;
import tacos.messaging.OrderEvent;

@Profile("jms-listener")
@Component
public class OrderListener {
  
  private KitchenUI ui;
  private final KitchenMetrics metrics;

  @Autowired
  public OrderListener(KitchenUI ui, KitchenMetrics metrics) {
    this.ui = ui;
    this.metrics = metrics;
  }

  @JmsListener(destination = "${tacocloud.messaging.jms.destination}")
  public void receiveOrder(OrderEvent event) {
    metrics.received(event, "jms");
    ui.displayOrder(event);
  }
  
}
