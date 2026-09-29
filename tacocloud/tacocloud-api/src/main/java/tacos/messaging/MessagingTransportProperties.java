package tacos.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("tacocloud.messaging")
public class MessagingTransportProperties {

  private Transport transport = Transport.NOOP;

  public Transport getTransport() {
    return transport;
  }

  public void setTransport(Transport transport) {
    this.transport = transport;
  }

  public enum Transport {
    NOOP, JMS, RABBIT, KAFKA
  }

}
