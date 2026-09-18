package tacos.physics;

import java.util.List;

public class TacoDesignValidationException extends RuntimeException {

  private static final long serialVersionUID = 1L;
  private final List<DesignViolation> violations;

  public TacoDesignValidationException(List<DesignViolation> violations) {
    super("Taco design violates " + violations.size() + " rule(s)");
    this.violations = violations;
  }

  public List<DesignViolation> getViolations() {
    return violations;
  }
}
