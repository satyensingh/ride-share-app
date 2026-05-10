package com.example.rideshare.controller;

import com.example.rideshare.dto.PublishRideRequest;
import com.example.rideshare.dto.PublishRideResponse;
import com.example.rideshare.service.RideService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ride")
@RequiredArgsConstructor
public class RideController {

    private final RideService rideService;

    @PostMapping("/publish")
    @ResponseStatus(HttpStatus.CREATED)
    public PublishRideResponse publishRide(@Valid @RequestBody PublishRideRequest request) {
        return rideService.publishRide(request);
    }
}
