package tacos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

public class UserPersistenceContractTest {

  private MappingMongoConverter converter;

  @BeforeEach
  public void setUp() throws Exception {
    MongoMappingContext context = new MongoMappingContext();
    context.setInitialEntitySet(Set.of(User.class));
    context.afterPropertiesSet();

    converter = new MappingMongoConverter(
        NoOpDbRefResolver.INSTANCE, context);
    converter.afterPropertiesSet();
  }

  @Test
  public void readsPersistedUserForAuthentication() {
    User original = new User(
        "qa_user_02",
        "encoded-password",
        "QA User",
        "Calle Prueba 123",
        "CDMX",
        "CDMX",
        "01234",
        "5551234567",
        "qa_user_02@example.com");

    Document document = new Document();
    converter.write(original, document);

    User restored = converter.read(User.class, document);

    assertEquals(original.getUsername(), restored.getUsername());
    assertEquals(original.getPassword(), restored.getPassword());
    assertEquals(original.getEmail(), restored.getEmail());
    assertTrue(restored.getAuthorities().stream()
        .anyMatch(authority -> authority.getAuthority().equals("ROLE_USER")));
  }
}
