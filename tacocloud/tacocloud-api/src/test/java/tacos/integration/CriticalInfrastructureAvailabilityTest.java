package tacos.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;

class CriticalInfrastructureAvailabilityTest {

  @Test
  void shouldRequireDockerInCi() {
    if ("true".equalsIgnoreCase(System.getenv("CI")))
      assertThat(DockerClientFactory.instance().isDockerAvailable()).isTrue();
  }
}
