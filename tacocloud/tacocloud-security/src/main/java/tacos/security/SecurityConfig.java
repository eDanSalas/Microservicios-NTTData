package tacos.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation
    .authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web
    .builders.HttpSecurity;
import org.springframework.security.config.annotation.web
    .configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web
    .configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory
    .PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication
    .HttpStatusEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.util.matcher
    .AntPathRequestMatcher;
import org.springframework.security.web.util.matcher
    .OrRequestMatcher;
import org.springframework.security.web.util.matcher
    .RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors
    .CorsConfigurationSource;
import org.springframework.web.cors
    .UrlBasedCorsConfigurationSource;

@SuppressWarnings("deprecation")
@Configuration
@EnableWebSecurity
public class SecurityConfig
    extends WebSecurityConfigurerAdapter {

  @Autowired
  private UserDetailsService userDetailsService;

  @Autowired(required = false)
  @Qualifier("apiProblemSecurityHandler")
  private AuthenticationEntryPoint apiAuthenticationEntryPoint;

  @Autowired(required = false)
  @Qualifier("apiProblemSecurityHandler")
  private AccessDeniedHandler apiAccessDeniedHandler;

  @Override
  protected void configure(
      HttpSecurity http)
      throws Exception {

    RequestMatcher nonBrowserRequest =
        new OrRequestMatcher(
            new AntPathRequestMatcher(
                "/api/**"),
            new AntPathRequestMatcher(
                "/data-api/**"),
            new AntPathRequestMatcher(
                "/actuator/**"));

    http
        .cors()
          .configurationSource(
              corsConfigurationSource())

      .and()
        .authorizeRequests()

          .antMatchers(
              HttpMethod.OPTIONS,
              "/**")
            .permitAll()

          .antMatchers(
              "/",
              "/home",
              "/login",
              "/register",
              "/csrf",
              "/openapi.yaml",
              "/error",
              "/index.html",
              "/favicon.ico",
              "/*.js",
              "/*.js.map",
              "/*.css",
              "/*.png",
              "/assets/**",
              "/images/**",
              "/css/**",
              "/js/**",
              "/webjars/**")
            .permitAll()

          .antMatchers(
              HttpMethod.GET,
              "/actuator/health",
              "/actuator/health/**")
            .permitAll()

          .antMatchers("/actuator/**")
            .hasRole("ADMIN")

          .antMatchers("/data-api/**")
            .hasRole("ADMIN")

          .antMatchers("/api/kitchen/**")
            .hasRole("KITCHEN")

          .antMatchers(
              "/api/users/me/favorites/**")
            .hasAnyRole(
                "USER",
                "ADMIN")

          .antMatchers(
              "/api/users/me/orders/**")
            .hasAnyRole(
                "USER",
                "ADMIN")

          .antMatchers(
              HttpMethod.GET,
              "/api/orders",
              "/api/orders/")
            .denyAll()

          .antMatchers(
              HttpMethod.PUT,
              "/api/tacos/*/rating")
            .hasAnyRole(
                "USER",
                "ADMIN")

          .antMatchers(
              HttpMethod.GET,
              "/api/tacos/top")
            .permitAll()

          .antMatchers(
              HttpMethod.GET,
              "/api/tacos/**",
              "/api/ingredients/**",
              "/api/announcements")
            .permitAll()

          .antMatchers("/api/admin/**")
            .hasRole("ADMIN")

          .antMatchers(
              HttpMethod.PATCH,
              "/api/orders/*/status")
            .hasAnyRole(
                "KITCHEN",
                "ADMIN")

          .antMatchers(
              HttpMethod.POST,
              "/api/orders/*/cancel")
            .hasAnyRole(
                "USER",
                "ADMIN")

          .antMatchers("/api/tacos/**")
            .hasRole("ADMIN")

          .antMatchers("/api/ingredients/**")
            .hasRole("ADMIN")

          .antMatchers("/api/orders/**")
            .hasAnyRole(
                "USER",
                "ADMIN")

          .antMatchers(
              "/design",
              "/orders/**")
            .hasAnyRole(
                "USER",
                "ADMIN")

          .antMatchers("/h2-console/**")
            .hasRole("ADMIN")

          .antMatchers(HttpMethod.POST,"/api/payment-methods/tokenize")
          .hasAnyRole("USER", "ADMIN")

          .anyRequest()
            .denyAll()

      .and()
        .exceptionHandling()
          .defaultAuthenticationEntryPointFor(
              apiAuthenticationEntryPoint != null ? apiAuthenticationEntryPoint
                  : new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
              nonBrowserRequest)
          .accessDeniedHandler(apiAccessDeniedHandler != null ? apiAccessDeniedHandler
              : new AccessDeniedHandlerImpl())

      .and()
        .formLogin()
          .loginPage("/login")
          .permitAll()

      .and()
        .httpBasic()
          .realmName("Taco Cloud")

      .and()
        .logout()
          .logoutSuccessUrl("/")
          .permitAll()

      .and()
        .csrf()
          .ignoringAntMatchers(
              "/h2-console/**")

      .and()
        .headers()
          .frameOptions()
            .sameOrigin();
  }

  @Bean
  public CorsConfigurationSource
      corsConfigurationSource() {

    CorsConfiguration configuration =
        new CorsConfiguration();

    configuration.setAllowedOrigins(
        List.of(
            "http://localhost:8080",
            "http://localhost:4200"));

    configuration.setAllowedMethods(
        List.of(
            "GET",
            "POST",
            "PUT",
            "PATCH",
            "DELETE",
            "OPTIONS"));

    configuration.setAllowedHeaders(
        List.of(
            "Authorization",
            "Content-Type",
            "Idempotency-Key",
            "X-Correlation-Id",
            "X-CSRF-TOKEN",
            "X-XSRF-TOKEN"));

    configuration.setExposedHeaders(
        List.of(
            "Deprecation",
            "Link",
            "Location",
            "Sunset",
            "X-Correlation-Id"));

    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source =
        new UrlBasedCorsConfigurationSource();

    source.registerCorsConfiguration(
        "/api/**",
        configuration);
    source.registerCorsConfiguration(
        "/register",
        configuration);
    source.registerCorsConfiguration(
        "/csrf",
        configuration);

    return source;
  }

  @Bean
  public PasswordEncoder encoder() {
    return PasswordEncoderFactories
        .createDelegatingPasswordEncoder();
  }

  @Override
  protected void configure(
      AuthenticationManagerBuilder auth)
      throws Exception {

    auth
        .userDetailsService(
            userDetailsService)
        .passwordEncoder(encoder());
  }
}
