package com.example.rideshare.dto;

public record CarDetailsResponse(
        String make,
        String model,
        String number,
        String color
) {
}
