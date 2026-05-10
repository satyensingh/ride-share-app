package com.example.rideshare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CarDetailsRequest(
        @NotBlank @Size(max = 100) String make,
        @NotBlank @Size(max = 100) String model,
        @NotBlank @Size(max = 30) String number,
        @Size(max = 50) String color
) {
}
