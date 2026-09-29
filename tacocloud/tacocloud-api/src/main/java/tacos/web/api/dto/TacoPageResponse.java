package tacos.web.api.dto;

import java.util.List;

import lombok.Value;

@Value
public class TacoPageResponse {
  List<TacoResponse> content;
  int page;
  int size;
  long totalElements;
  int totalPages;
  String sort;
}
