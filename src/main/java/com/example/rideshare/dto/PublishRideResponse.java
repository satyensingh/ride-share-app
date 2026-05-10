package com.example.rideshare.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PublishRideResponse(
        Long rideId,
        String source,
        String destination,
        Instant departureTime,
        Integer availableSeats,
        BigDecimal pricePerSeat,
        CarDetailsResponse carDetails,
        String status,
        Instant createdAt
) {
}
