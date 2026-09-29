package tacos.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import tacos.web.api.dto.OrderCreateRequest;

@Component
public class OrderRequestHasher {

  public String hash(OrderCreateRequest request) {
    StringBuilder canonical = new StringBuilder();
    Stream.of(request.getDeliveryName(), request.getDeliveryStreet(), request.getDeliveryCity(),
        request.getDeliveryState(), request.getDeliveryZip(), request.getPaymentMethodId(),
        normalizeCoupon(request.getCouponCode())).forEach(value -> append(canonical, value));
    append(canonical, request.getItems().size());
    request.getItems().forEach(item -> {
      append(canonical, item.getQuantity());
      append(canonical, item.getTaco().getName());
      append(canonical, item.getTaco().getIngredientIds().size());
      item.getTaco().getIngredientIds().stream().sorted().forEach(value -> append(canonical, value));
    });
    return digest(canonical.toString());
  }

  public String scopedId(String userId, String key) {
    return digest(userId + "\u0000" + key);
  }

  private String normalizeCoupon(String value) {
    return value == null ? null : value.trim().toUpperCase();
  }

  private void append(StringBuilder target, Object value) {
    String text = value == null ? "" : value.toString();
    target.append(text.length()).append(':').append(text);
  }

  private String digest(String value) {
    try {
      byte[] bytes = MessageDigest.getInstance("SHA-256")
          .digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder result = new StringBuilder(bytes.length * 2);
      for (byte current : bytes) result.append(String.format("%02x", current));
      return result.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
