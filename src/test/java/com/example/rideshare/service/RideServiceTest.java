package com.example.rideshare.service;

import com.example.rideshare.dto.CarDetailsRequest;
import com.example.rideshare.dto.PublishRideRequest;
import com.example.rideshare.dto.PublishRideResponse;
import com.example.rideshare.entity.Ride;
import com.example.rideshare.entity.RideStatus;
import com.example.rideshare.repository.RideRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RideServiceTest {

    private static final Instant DEPARTURE_TIME = Instant.parse("2026-06-15T09:30:00Z");
    private static final Instant CREATED_AT = Instant.parse("2026-05-10T10:00:00Z");

    @Mock
    private RideRepository rideRepository;

    @InjectMocks
    private RideService rideService;

    @Test
    void publishRideShouldNormalizeAndSaveRide() {
        PublishRideRequest request = new PublishRideRequest(
                "  Navi Mumbai  ",
                "  Pune  ",
                DEPARTURE_TIME,
                3,
                BigDecimal.valueOf(450.00),
                new CarDetailsRequest("  Maruti Suzuki  ", "  Ertiga  ", "  mh46ab1234  ", "  White  ")
        );

        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> {
            Ride ride = invocation.getArgument(0);
            ride.setId(1L);
            ride.setCreatedAt(CREATED_AT);
            return ride;
        });

        PublishRideResponse response = rideService.publishRide(request);

        ArgumentCaptor<Ride> rideCaptor = ArgumentCaptor.forClass(Ride.class);
        verify(rideRepository).save(rideCaptor.capture());
        Ride savedRide = rideCaptor.getValue();

        assertAll(
                () -> assertThat(savedRide.getSource()).isEqualTo("Navi Mumbai"),
                () -> assertThat(savedRide.getDestination()).isEqualTo("Pune"),
                () -> assertThat(savedRide.getDepartureTime()).isEqualTo(DEPARTURE_TIME),
                () -> assertThat(savedRide.getAvailableSeats()).isEqualTo(3),
                () -> assertThat(savedRide.getPricePerSeat()).isEqualByComparingTo("450.0"),
                () -> assertThat(savedRide.getCarMake()).isEqualTo("Maruti Suzuki"),
                () -> assertThat(savedRide.getCarModel()).isEqualTo("Ertiga"),
                () -> assertThat(savedRide.getCarNumber()).isEqualTo("MH46AB1234"),
                () -> assertThat(savedRide.getCarColor()).isEqualTo("White"),
                () -> assertThat(savedRide.getStatus()).isEqualTo(RideStatus.PUBLISHED)
        );

        assertAll(
                () -> assertThat(response.rideId()).isEqualTo(1L),
                () -> assertThat(response.source()).isEqualTo("Navi Mumbai"),
                () -> assertThat(response.destination()).isEqualTo("Pune"),
                () -> assertThat(response.departureTime()).isEqualTo(DEPARTURE_TIME),
                () -> assertThat(response.availableSeats()).isEqualTo(3),
                () -> assertThat(response.pricePerSeat()).isEqualByComparingTo("450.0"),
                () -> assertThat(response.carDetails().make()).isEqualTo("Maruti Suzuki"),
                () -> assertThat(response.carDetails().model()).isEqualTo("Ertiga"),
                () -> assertThat(response.carDetails().number()).isEqualTo("MH46AB1234"),
                () -> assertThat(response.carDetails().color()).isEqualTo("White"),
                () -> assertThat(response.status()).isEqualTo("PUBLISHED"),
                () -> assertThat(response.createdAt()).isEqualTo(CREATED_AT)
        );
    }

    @Test
    void publishRideShouldAllowMissingCarColor() {
        PublishRideRequest request = new PublishRideRequest(
                "Navi Mumbai",
                "Pune",
                DEPARTURE_TIME,
                1,
                BigDecimal.valueOf(250.00),
                new CarDetailsRequest("Honda", "City", "MH46AB1234", null)
        );

        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> {
            Ride ride = invocation.getArgument(0);
            ride.setId(2L);
            ride.setCreatedAt(CREATED_AT);
            return ride;
        });

        PublishRideResponse response = rideService.publishRide(request);

        ArgumentCaptor<Ride> rideCaptor = ArgumentCaptor.forClass(Ride.class);
        verify(rideRepository).save(rideCaptor.capture());

        assertAll(
                () -> assertThat(rideCaptor.getValue().getCarColor()).isNull(),
                () -> assertThat(response.rideId()).isEqualTo(2L),
                () -> assertThat(response.carDetails().color()).isNull()
        );
    }
}
