package tacos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.Ingredient;
import tacos.Ingredient.Type;
import tacos.data.IngredientRepository;
import tacos.web.api.dto.CatalogUpdateRequest;
import tacos.web.api.dto.StockAdjustmentRequest;

public class CatalogInventoryServiceTest {

  private IngredientRepository repo;
  private CatalogInventoryService service;

  @BeforeEach
  public void setUp() {
    repo = Mockito.mock(IngredientRepository.class);
    service = new CatalogInventoryService(repo);
  }

  @Test
  public void shouldRoundPriceAndAllowCommercialPauseWithStock() {
    Ingredient ingredient = ingredient(10, false, 3L);
    CatalogUpdateRequest request = new CatalogUpdateRequest();
    request.setUnitPrice(new BigDecimal("12.345"));
    request.setAvailable(false);
    request.setVersion(3L);
    when(repo.findById("FLTO")).thenReturn(Mono.just(ingredient));
    when(repo.save(ingredient)).thenReturn(Mono.just(ingredient));

    StepVerifier.create(service.updateCatalog("FLTO", request))
        .assertNext(saved -> {
          assertEquals(new BigDecimal("12.35"), saved.getUnitPrice());
          assertEquals(10, saved.getStockOnHand());
          assertFalse(saved.isAvailable());
        })
        .verifyComplete();
  }

  @Test
  public void shouldRejectAdjustmentThatMakesStockNegative() {
    Ingredient ingredient = ingredient(2, true, 1L);
    StockAdjustmentRequest request = stockAdjustment(-3, 1L);
    when(repo.findById("FLTO")).thenReturn(Mono.just(ingredient));

    StepVerifier.create(service.adjustStock("FLTO", request))
        .expectErrorSatisfies(error -> assertStatus(error, HttpStatus.UNPROCESSABLE_ENTITY))
        .verify();

    verify(repo, never()).save(ingredient);
  }

  @Test
  public void shouldRejectStaleVersion() {
    Ingredient ingredient = ingredient(10, true, 5L);
    when(repo.findById("FLTO")).thenReturn(Mono.just(ingredient));

    StepVerifier.create(service.adjustStock("FLTO", stockAdjustment(1, 4L)))
        .expectErrorSatisfies(error -> assertStatus(error, HttpStatus.CONFLICT))
        .verify();

    verify(repo, never()).save(ingredient);
  }

  @Test
  public void shouldTranslateOptimisticLockFailureToConflict() {
    Ingredient ingredient = ingredient(10, true, 5L);
    when(repo.findById("FLTO")).thenReturn(Mono.just(ingredient));
    when(repo.save(ingredient)).thenReturn(Mono.error(new OptimisticLockingFailureException("stale")));

    StepVerifier.create(service.adjustStock("FLTO", stockAdjustment(1, 5L)))
        .expectErrorSatisfies(error -> assertStatus(error, HttpStatus.CONFLICT))
        .verify();
  }

  private Ingredient ingredient(int stock, boolean available, Long version) {
    return new Ingredient("FLTO", "Flour Tortilla", Type.WRAP,
        new BigDecimal("0.75"), available, stock, 2, version);
  }

  private StockAdjustmentRequest stockAdjustment(int quantity, Long version) {
    StockAdjustmentRequest request = new StockAdjustmentRequest();
    request.setQuantity(quantity);
    request.setVersion(version);
    return request;
  }

  private void assertStatus(Throwable error, HttpStatus status) {
    assertEquals(ResponseStatusException.class, error.getClass());
    assertEquals(status, ((ResponseStatusException) error).getStatus());
  }
}
