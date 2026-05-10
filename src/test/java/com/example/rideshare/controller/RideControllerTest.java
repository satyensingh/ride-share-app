package com.example.rideshare.controller;

import com.example.rideshare.dto.*;
import com.example.rideshare.service.RideService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RideController.class)
class RideControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RideService rideService;

    @Test
    void publishRideShouldReturnCreated() throws Exception {
        PublishRideRequest request = new PublishRideRequest(
                "Navi Mumbai",
                "Pune",
                Instant.parse("2026-06-15T09:30:00Z"),
                3,
                BigDecimal.valueOf(450.00),
                new CarDetailsRequest("Maruti Suzuki", "Ertiga", "MH46AB1234", "White")
        );

        PublishRideResponse response = new PublishRideResponse(
                1L,
                "Navi Mumbai",
                "Pune",
                Instant.parse("2026-06-15T09:30:00Z"),
                3,
                BigDecimal.valueOf(450.00),
                new CarDetailsResponse("Maruti Suzuki", "Ertiga", "MH46AB1234", "White"),
                "PUBLISHED",
                Instant.parse("2026-05-10T10:00:00Z")
        );

        when(rideService.publishRide(any(PublishRideRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/ride/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rideId").value(1))
                .andExpect(jsonPath("$.source").value("Navi Mumbai"))
                .andExpect(jsonPath("$.destination").value("Pune"))
                .andExpect(jsonPath("$.availableSeats").value(3))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.carDetails.number").value("MH46AB1234"));
    }

    @Test
    void publishRideShouldReturnBadRequestForInvalidPayload() throws Exception {
        String invalidPayload = """
                {
                  "source": "",
                  "destination": "Pune",
                  "departureTime": "2020-06-15T09:30:00Z",
                  "numberOfSeats": 0,
                  "pricePerSeat": -1,
                  "carDetails": {
                    "make": "",
                    "model": "Ertiga",
                    "number": "MH46AB1234"
                  }
                }
                """;

        mockMvc.perform(post("/api/v1/ride/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }
}
