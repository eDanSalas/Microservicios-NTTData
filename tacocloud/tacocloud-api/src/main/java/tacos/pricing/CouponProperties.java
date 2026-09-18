package tacos.pricing;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "tacocloud.coupons")
public class CouponProperties {

  private Map<String, CouponRule> codes = new HashMap<>();
}
