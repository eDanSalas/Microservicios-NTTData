package tacos.web.api;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import lombok.Data;

@Data
public class EmailOrder {

  @NotBlank
  @Email
  @Size(max = 254)
  private String email;

  @Valid
  @NotNull
  @Size(min = 1, max = 20)
  private List<EmailTaco> tacos =
      new ArrayList<>();

  @Data
  public static class EmailTaco {

    @NotBlank
    @Size(min = 5, max = 50)
    private String name;

    @NotNull
    @Size(min = 1, max = 10)
    private List<
        @NotBlank
        @Pattern(
            regexp = "[A-Za-z0-9_-]+")
        String> ingredients =
            new ArrayList<>();
  }
}