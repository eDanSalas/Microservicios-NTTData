package tacos.web.api.dto;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiProblem {

  private URI type;
  private String title;
  private int status;
  private String detail;
  private URI instance;
  private String code;

  @Builder.Default
  private List<ApiViolation> violations = new ArrayList<>();
}