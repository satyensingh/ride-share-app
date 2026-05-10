package com.example.rideshare.service;

import com.example.rideshare.dto.*;
import com.example.rideshare.entity.Ride;
import com.example.rideshare.entity.RideStatus;
import com.example.rideshare.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RideService {

    private final RideRepository rideRepository;

    @Transactional
    public PublishRideResponse publishRide(PublishRideRequest request) {
        Ride ride = Ride.builder()
                .source(request.source().trim())
                .destination(request.destination().trim())
                .departureTime(request.departureTime())
                .availableSeats(request.numberOfSeats())
                .pricePerSeat(request.pricePerSeat())
                .carMake(request.carDetails().make().trim())
                .carModel(request.carDetails().model().trim())
                .carNumber(request.carDetails().number().trim().toUpperCase())
                .carColor(request.carDetails().color() == null ? null : request.carDetails().color().trim())
                .status(RideStatus.PUBLISHED)
                .build();

        Ride saved = rideRepository.save(ride);
        return toResponse(saved);
    }

    private PublishRideResponse toResponse(Ride ride) {
        return new PublishRideResponse(
                ride.getId(),
                ride.getSource(),
                ride.getDestination(),
                ride.getDepartureTime(),
                ride.getAvailableSeats(),
                ride.getPricePerSeat(),
                new CarDetailsResponse(
                        ride.getCarMake(),
                        ride.getCarModel(),
                        ride.getCarNumber(),
                        ride.getCarColor()
                ),
                ride.getStatus().name(),
                ride.getCreatedAt()
        );
    }
}
