package tacos.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;

class CriticalInfrastructureAvailabilityTest {

  @Test
  void shouldRequireDockerInCiOrDockerRunner() {
    if ("true".equalsIgnoreCase(System.getenv("CI"))
        || "true".equalsIgnoreCase(System.getenv("TACOCLOUD_REQUIRE_DOCKER")))
      assertThat(DockerClientFactory.instance().isDockerAvailable())
          .as("Docker must be available for TC-36 integration tests").isTrue();
  }
}
