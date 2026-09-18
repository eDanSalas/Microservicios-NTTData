package tacos.web.api.dto;

import java.util.List;

import lombok.Value;
import tacos.physics.DesignViolation;

@Value
public class TacoValidationResponse {
  boolean valid;
  List<DesignViolation> violations;
}
