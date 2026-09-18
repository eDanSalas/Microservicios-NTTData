package tacos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.Ingredient;
import tacos.Ingredient.Type;
import tacos.data.IngredientRepository;
import tacos.physics.TacoDesignValidator;

public class TacoDesignServiceTest {

  @Test
  public void shouldResolveEachUniqueIngredientOnlyOnceAndPreserveDuplicates() {
    IngredientRepository repo = Mockito.mock(IngredientRepository.class);
    Ingredient ingredient = new Ingredient("WRAP", "Wrap", Type.WRAP);
    when(repo.findById("WRAP")).thenReturn(Mono.just(ingredient));
    TacoDesignService service = new TacoDesignService(repo, new TacoDesignValidator(List.of()));

    StepVerifier.create(service.resolve("Duplicate", List.of("WRAP", "WRAP")))
        .assertNext(taco -> assertThat(taco.getIngredients()).containsExactly(ingredient, ingredient))
        .verifyComplete();
    verify(repo, times(1)).findById("WRAP");
  }
}
