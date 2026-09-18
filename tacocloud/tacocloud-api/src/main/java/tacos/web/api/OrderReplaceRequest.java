package tacos.web.api;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import lombok.Getter;
import lombok.Setter;
import tacos.Taco;

@Getter
@Setter
public class OrderReplaceRequest {

    @NotBlank
    @Size(max = 100)
    @JsonSetter(nulls = Nulls.FAIL)
    private String deliveryName;

    @NotBlank
    @Size(max = 120)
    @JsonSetter(nulls = Nulls.FAIL)
    private String deliveryStreet;

    @NotBlank
    @Size(max = 80)
    @JsonSetter(nulls = Nulls.FAIL)
    private String deliveryCity;

    @NotBlank
    @Size(max = 80)
    @JsonSetter(nulls = Nulls.FAIL)
    private String deliveryState;

    @NotBlank
    @Pattern(
        regexp = "\\d{5}",
        message = "must contain exactly 5 digits")
    @JsonSetter(nulls = Nulls.FAIL)
    private String deliveryZip;

    @Valid
    @NotNull
    @Size(
        min = 1,
        max = 20,
        message = "must contain between 1 and 20 tacos")
    @JsonSetter(nulls = Nulls.FAIL)
    private List<Taco> tacos = new ArrayList<>();

    @JsonAnySetter
    public void rejectUnknownProperty(
            String propertyName,
            Object value) {

        throw new IllegalArgumentException(
            "Unknown order property: " + propertyName);
    }
}