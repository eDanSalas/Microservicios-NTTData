package tacos.web.api;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;
import tacos.service.CatalogInventoryService;
import tacos.web.api.dto.AdminIngredientResponse;
import tacos.web.api.dto.CatalogUpdateRequest;
import tacos.web.api.dto.StockAdjustmentRequest;
import tacos.web.api.mapper.IngredientMapper;

@RestController
@RequestMapping(path = "/api/admin/ingredients", produces = "application/json")
public class AdminIngredientController {

  private final CatalogInventoryService service;
  private final IngredientMapper mapper;

  public AdminIngredientController(CatalogInventoryService service, IngredientMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  @PatchMapping("/{id}/catalog")
  public Mono<ResponseEntity<AdminIngredientResponse>> updateCatalog(@PathVariable String id,
      @Valid @RequestBody CatalogUpdateRequest request) {
    return service.updateCatalog(id, request).map(mapper::toAdminResponse).map(ResponseEntity::ok);
  }

  @PostMapping("/{id}/stock-adjustments")
  public Mono<ResponseEntity<AdminIngredientResponse>> adjustStock(@PathVariable String id,
      @Valid @RequestBody StockAdjustmentRequest request) {
    return service.adjustStock(id, request).map(mapper::toAdminResponse).map(ResponseEntity::ok);
  }
}
