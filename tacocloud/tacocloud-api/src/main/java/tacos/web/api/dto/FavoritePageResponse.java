package tacos.web.api.dto;

import java.util.List;

import lombok.Value;

@Value
public class FavoritePageResponse {
  List<TacoResponse> content;
  int page;
  int size;
  long totalElements;
  int totalPages;
}
