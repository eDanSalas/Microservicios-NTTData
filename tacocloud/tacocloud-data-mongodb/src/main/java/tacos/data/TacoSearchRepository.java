package tacos.data;

import reactor.core.publisher.Mono;

public interface TacoSearchRepository {
  Mono<TacoSearchPage> search(TacoSearchQuery query);
}
