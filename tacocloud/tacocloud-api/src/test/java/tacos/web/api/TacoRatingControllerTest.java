package tacos.web.api;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.reactive.result.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.Taco;
import tacos.User;
import tacos.service.TacoClassificationService;
import tacos.service.TacoRatingService;
import tacos.service.TacoRatingStats;
import tacos.web.api.error.GlobalApiExceptionHandler;
import tacos.web.api.mapper.IngredientMapper;
import tacos.web.api.mapper.TacoMapper;

public class TacoRatingControllerTest {
  private TacoRatingService service;
  private User user;
  private WebTestClient client;

  @BeforeEach
  public void setUp() {
    service = Mockito.mock(TacoRatingService.class);
    user = Mockito.mock(User.class);
    when(user.getId()).thenReturn("user-a");
    Mockito.doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(user).getAuthorities();
    Authentication authentication = new UsernamePasswordAuthenticationToken(user, null,
        user.getAuthorities());
    TacoMapper mapper = new TacoMapper(new IngredientMapper(), new TacoClassificationService());
    client = WebTestClient.bindToController(new TacoRatingController(service, mapper))
        .controllerAdvice(new GlobalApiExceptionHandler()).apply(springSecurity())
        .argumentResolvers(configurer -> configurer.addCustomResolver(
            new AuthenticationPrincipalArgumentResolver(ReactiveAdapterRegistry.getSharedInstance())))
        .build().mutateWith(mockAuthentication(authentication));
  }

  @Test
  public void shouldRateAsAuthenticatedUser() {
    when(service.rate(same(user), Mockito.eq("taco-1"), Mockito.eq(5)))
        .thenReturn(Mono.just(stats("taco-1", "4.50", 2)));

    client.put().uri("/api/tacos/taco-1/rating").contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"score\":5}").exchange().expectStatus().isOk().expectBody()
        .jsonPath("$.taco.id").isEqualTo("taco-1").jsonPath("$.average").isEqualTo(4.5)
        .jsonPath("$.count").isEqualTo(2);
    verify(service).rate(same(user), Mockito.eq("taco-1"), Mockito.eq(5));
  }

  @Test
  public void shouldRejectScoreOutsideRange() {
    client.put().uri("/api/tacos/taco-1/rating").contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"score\":6}").exchange().expectStatus().isBadRequest();
    verify(service, never()).rate(Mockito.any(), Mockito.anyString(), Mockito.anyInt());
  }

  @Test
  public void shouldReturnTopRanking() {
    when(service.top(5)).thenReturn(Flux.just(stats("taco-1", "4.50", 4)));

    client.get().uri("/api/tacos/top?limit=5").exchange().expectStatus().isOk().expectBody()
        .jsonPath("$[0].taco.id").isEqualTo("taco-1").jsonPath("$[0].count").isEqualTo(4);
    verify(service).top(5);
  }

  private TacoRatingStats stats(String id, String average, long count) {
    Taco taco = new Taco();
    taco.setId(id);
    taco.setName("Rated taco");
    taco.setIngredients(List.of());
    return new TacoRatingStats(taco, new BigDecimal(average), count,
        Map.of(1, 0L, 2, 0L, 3, 0L, 4, 1L, 5, 1L));
  }
}
