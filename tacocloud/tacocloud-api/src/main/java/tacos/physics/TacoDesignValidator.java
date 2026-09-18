package tacos.physics;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import tacos.Taco;

@Service
public class TacoDesignValidator {

  private final List<TacoDesignRule> rules;

  public TacoDesignValidator(List<TacoDesignRule> rules) {
    this.rules = rules;
  }

  public List<DesignViolation> validate(Taco taco) {
    return rules.stream().flatMap(rule -> rule.validate(taco).stream())
        .sorted(Comparator.comparing(DesignViolation::getCode).thenComparing(DesignViolation::getMessage))
        .collect(Collectors.toList());
  }

  public Taco requireValid(Taco taco) {
    List<DesignViolation> violations = validate(taco);
    if (!violations.isEmpty()) throw new TacoDesignValidationException(violations);
    return taco;
  }
}
