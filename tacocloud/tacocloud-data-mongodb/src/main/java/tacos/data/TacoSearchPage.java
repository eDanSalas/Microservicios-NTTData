package tacos.data;

import java.util.List;

import lombok.Value;
import tacos.Taco;

@Value
public class TacoSearchPage {
  List<Taco> content;
  int page;
  int size;
  long totalElements;
  int totalPages;
}
