package com.example.rideshare.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

public record PublishRideRequest(
        @NotBlank @Size(max = 200) String source,
        @NotBlank @Size(max = 200) String destination,
        @NotNull @Future Instant departureTime,
        @NotNull @Min(1) @Max(8) Integer numberOfSeats,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal pricePerSeat,
        @NotNull @Valid CarDetailsRequest carDetails
) {
}
