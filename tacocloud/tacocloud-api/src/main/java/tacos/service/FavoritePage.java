package tacos.service;

import java.util.List;

import lombok.Value;
import tacos.Taco;

@Value
public class FavoritePage {
  List<Taco> content;
  int page;
  int size;
  long totalElements;
  int totalPages;
}
