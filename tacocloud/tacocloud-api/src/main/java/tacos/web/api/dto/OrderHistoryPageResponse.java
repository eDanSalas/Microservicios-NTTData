package tacos.web.api.dto;

import java.util.List;

import lombok.Value;

@Value
public class OrderHistoryPageResponse<T> {
  List<T> content;
  int page;
  int size;
  long totalElements;
  int totalPages;
}
