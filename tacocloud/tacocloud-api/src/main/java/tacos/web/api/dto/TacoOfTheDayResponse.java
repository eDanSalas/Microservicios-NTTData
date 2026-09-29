package tacos.web.api.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Value;

@Value
public class TacoOfTheDayResponse {
  TacoResponse taco;
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  LocalDate date;
  String reason;
}
