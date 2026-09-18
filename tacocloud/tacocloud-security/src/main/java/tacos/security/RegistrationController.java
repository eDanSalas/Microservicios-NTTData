package tacos.security;

import javax.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/register")
public class RegistrationController {

  private final RegistrationService registrationService;

  public RegistrationController(RegistrationService registrationService) {
    this.registrationService = registrationService;
  }

  @GetMapping
  public String registerForm(Model model) {
    model.addAttribute(
        "registrationForm",
        new RegistrationForm());

    return "registration";
  }

  @PostMapping
  public Mono<String> processRegistration(@Valid @ModelAttribute("registrationForm") RegistrationForm form, BindingResult validation) {
    if (validation.hasErrors()) {
      return Mono.just("registration");
    }
    return registrationService
        .register(form)
        .thenReturn("redirect:/login");
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public Mono<ResponseEntity<Void>> processJsonRegistration(@Valid @RequestBody RegistrationForm form) {
    return registrationService.register(form)
        .thenReturn(ResponseEntity.status(HttpStatus.CREATED).<Void>build())
        .onErrorReturn(RegistrationConflictException.class, ResponseEntity.status(HttpStatus.CONFLICT).build());
  }
}
