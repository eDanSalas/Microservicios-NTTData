package tacos.web.api;

import javax.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.pricing.CouponQuote;
import tacos.pricing.CouponService;
import tacos.service.OrderPricingService;
import tacos.service.TacoClassificationService;
import tacos.service.TacoDesignService;
import tacos.web.api.dto.OrderItemRequest;
import tacos.web.api.dto.OrderQuoteRequest;
import tacos.web.api.dto.OrderQuoteResponse;

@RestController
@RequestMapping(path = "/api/orders", produces = "application/json")
public class OrderQuoteController {

  private final CouponService couponService;
  private final OrderPricingService pricingService;
  private final TacoClassificationService classificationService;
  private final TacoDesignService designService;

  public OrderQuoteController(CouponService couponService, OrderPricingService pricingService,
      TacoClassificationService classificationService, TacoDesignService designService) {
    this.couponService = couponService;
    this.pricingService = pricingService;
    this.classificationService = classificationService;
    this.designService = designService;
  }

  @PostMapping(path = "/quote", consumes = "application/json")
  public Mono<OrderQuoteResponse> quote(@Valid @RequestBody OrderQuoteRequest request) {
    return Flux.fromIterable(request.getItems()).concatMap(this::classify).collectList()
        .map(classifications -> response(request, classifications));
  }

  private OrderQuoteResponse response(OrderQuoteRequest request,
      java.util.List<tacos.web.api.dto.TacoClassificationResponse> classifications) {
    CouponQuote quote = couponService.quote(request.getSubtotal(), request.getCouponCode());
    return new OrderQuoteResponse(quote.getCode(), quote.getSubtotal(), quote.getDiscount(), quote.getTotal(),
        pricingService.getCurrency(), classifications);
  }

  private Mono<tacos.web.api.dto.TacoClassificationResponse> classify(OrderItemRequest item) {
    return designService.resolveAndRequireValid(item.getTaco().getName(), item.getTaco().getIngredientIds())
        .map(classificationService::classify);
  }
}
