package tacos;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Document
@CompoundIndex(name = "uk_favorite_user_taco", def = "{'userId': 1, 'tacoId': 1}", unique = true)
public class Favorite {
  @Id
  private String id;
  private String userId;
  private String tacoId;
  private Date createdAt = new Date();

  public Favorite(String userId, String tacoId) {
    this.userId = userId;
    this.tacoId = tacoId;
  }
}
