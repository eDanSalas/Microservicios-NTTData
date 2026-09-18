package tacos.web.api;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class OrderPatchRequest {
    @JsonSetter(nulls = Nulls.FAIL)
    @Pattern(
        regexp = ".*\\S.*",
        message = "must not be blank")
    @Size(max = 100)
    private String deliveryName;

    @JsonSetter(nulls = Nulls.FAIL)
    @Pattern(
        regexp = ".*\\S.*",
        message = "must not be blank")
    @Size(max = 120)
    private String deliveryStreet;

    @JsonSetter(nulls = Nulls.FAIL)
    @Pattern(
        regexp = ".*\\S.*",
        message = "must not be blank")
    @Size(max = 80)
    private String deliveryCity;

    @JsonSetter(nulls = Nulls.FAIL)
    @Pattern(
        regexp = ".*\\S.*",
        message = "must not be blank")
    @Size(max = 80)
    private String deliveryState;

    @JsonSetter(nulls = Nulls.FAIL)
    @Pattern(
        regexp = "\\d{5}",
        message = "must contain exactly 5 digits")
    private String deliveryZip;

    @AssertTrue(message = "at least one delivery field must be provided")
    public boolean isAtLeastOneFieldPresent() {
        return deliveryName != null
            || deliveryStreet != null
            || deliveryCity != null
            || deliveryState != null
            || deliveryZip != null;
    }

    @JsonAnySetter
    public void rejectUnknownProperty(String propertyName, Object value) {
        throw new IllegalArgumentException(
            "Unknown patch property: " + propertyName);
    }
}