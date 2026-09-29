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
@CompoundIndex(name = "uk_rating_user_taco", def = "{'userId': 1, 'tacoId': 1}", unique = true)
public class TacoRating {
  @Id
  private String id;
  private String userId;
  private String tacoId;
  private int score;
  private Date createdAt = new Date();
  private Date updatedAt = new Date();

  public TacoRating(String userId, String tacoId, int score) {
    this.userId = userId;
    this.tacoId = tacoId;
    this.score = score;
  }
}
