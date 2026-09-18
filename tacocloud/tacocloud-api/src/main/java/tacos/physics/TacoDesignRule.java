package tacos.physics;

import java.util.List;

import tacos.Taco;

public interface TacoDesignRule {
  List<DesignViolation> validate(Taco taco);
}
