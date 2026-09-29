package tacos.web.api;

import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockAuthentication;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.reactive.result.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;
import tacos.Taco;
import tacos.User;
import tacos.service.FavoritePage;
import tacos.service.FavoriteService;
import tacos.service.TacoClassificationService;
import tacos.web.api.error.GlobalApiExceptionHandler;
import tacos.web.api.mapper.IngredientMapper;
import tacos.web.api.mapper.TacoMapper;

public class FavoriteControllerTest {
  private FavoriteService service;
  private User user;
  private WebTestClient client;

  @BeforeEach
  public void setUp() {
    service = Mockito.mock(FavoriteService.class);
    user = Mockito.mock(User.class);
    when(user.getId()).thenReturn("user-a");
    Mockito.doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER"))).when(user).getAuthorities();
    Authentication authentication = new UsernamePasswordAuthenticationToken(user, null,
        user.getAuthorities());
    TacoMapper mapper = new TacoMapper(new IngredientMapper(), new TacoClassificationService());
    client = WebTestClient.bindToController(new FavoriteController(service, mapper))
        .controllerAdvice(new GlobalApiExceptionHandler()).apply(springSecurity())
        .argumentResolvers(configurer -> configurer.addCustomResolver(
            new AuthenticationPrincipalArgumentResolver(ReactiveAdapterRegistry.getSharedInstance())))
        .build().mutateWith(mockAuthentication(authentication));
  }

  @Test
  public void shouldAddFavoriteForAuthenticatedUser() {
    when(service.add(same(user), Mockito.eq("taco-1"))).thenReturn(Mono.just(taco("taco-1")));

    client.put().uri("/api/users/me/favorites/taco-1").exchange().expectStatus().isOk()
        .expectBody().jsonPath("$.id").isEqualTo("taco-1");
    verify(service).add(same(user), Mockito.eq("taco-1"));
  }

  @Test
  public void shouldDeleteFavoriteIdempotently() {
    when(service.remove(same(user), Mockito.eq("taco-1"))).thenReturn(Mono.empty());

    client.delete().uri("/api/users/me/favorites/taco-1").exchange().expectStatus().isNoContent();
    verify(service).remove(same(user), Mockito.eq("taco-1"));
  }

  @Test
  public void shouldListPaginatedFavoritesForAuthenticatedUser() {
    when(service.findAll(same(user), Mockito.eq(1), Mockito.eq(5)))
        .thenReturn(Mono.just(new FavoritePage(List.of(taco("taco-1")), 1, 5, 6, 2)));

    client.get().uri("/api/users/me/favorites?page=1&size=5").exchange().expectStatus().isOk()
        .expectBody().jsonPath("$.content[0].id").isEqualTo("taco-1")
        .jsonPath("$.page").isEqualTo(1).jsonPath("$.totalElements").isEqualTo(6);
    verify(service).findAll(same(user), Mockito.eq(1), Mockito.eq(5));
  }

  private Taco taco(String id) {
    Taco taco = new Taco();
    taco.setId(id);
    taco.setName("Favorite taco");
    taco.setIngredients(List.of());
    return taco;
  }
}
