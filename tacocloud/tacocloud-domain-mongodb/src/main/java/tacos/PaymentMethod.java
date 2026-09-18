package tacos;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@Document(collection = "paymentMethod")
public class PaymentMethod {
  @Id
  private String id;

  @JsonIgnore
  @ToString.Exclude
  private String userId;

  @JsonIgnore
  @ToString.Exclude
  private String paymentToken;

  private String brand;
  private String last4;
  private int expirationMonth;
  private int expirationYear;
  private Date createdAt = new Date();
}