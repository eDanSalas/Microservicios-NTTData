package tacos.service;

import java.util.List;

import lombok.Value;
import tacos.TacoOrder;

@Value
public class OrderHistoryPage {
  List<TacoOrder> content;
  int page;
  int size;
  long totalElements;
  int totalPages;
}
