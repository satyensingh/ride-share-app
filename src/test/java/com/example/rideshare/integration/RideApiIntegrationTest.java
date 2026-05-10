package com.example.rideshare.integration;

import com.example.rideshare.dto.CarDetailsRequest;
import com.example.rideshare.dto.PublishRideRequest;
import com.example.rideshare.dto.PublishRideResponse;
import com.example.rideshare.entity.Ride;
import com.example.rideshare.entity.RideStatus;
import com.example.rideshare.exception.ApiErrorResponse;
import com.example.rideshare.repository.RideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RideApiIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ride_share_test")
            .withUsername("rideshare_user")
            .withPassword("rideshare_pass");

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RideRepository rideRepository;

    @BeforeEach
    void cleanDatabase() {
        rideRepository.deleteAll();
    }

    @Test
    void publishRideShouldPersistRideAndReturnCreatedResponse() {
        PublishRideRequest request = new PublishRideRequest(
                "  Navi Mumbai  ",
                "  Pune  ",
                Instant.parse("2026-06-15T09:30:00Z"),
                3,
                BigDecimal.valueOf(450.00),
                new CarDetailsRequest("  Maruti Suzuki  ", "  Ertiga  ", "  mh46ab1234  ", "  White  ")
        );

        ResponseEntity<PublishRideResponse> response = restTemplate.postForEntity(
                "/api/v1/ride/publish",
                request,
                PublishRideResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        PublishRideResponse body = response.getBody();
        assertThat(body).isNotNull();

        assertAll(
                () -> assertThat(body.rideId()).isNotNull(),
                () -> assertThat(body.source()).isEqualTo("Navi Mumbai"),
                () -> assertThat(body.destination()).isEqualTo("Pune"),
                () -> assertThat(body.departureTime()).isEqualTo(Instant.parse("2026-06-15T09:30:00Z")),
                () -> assertThat(body.availableSeats()).isEqualTo(3),
                () -> assertThat(body.pricePerSeat()).isEqualByComparingTo("450.00"),
                () -> assertThat(body.carDetails().make()).isEqualTo("Maruti Suzuki"),
                () -> assertThat(body.carDetails().model()).isEqualTo("Ertiga"),
                () -> assertThat(body.carDetails().number()).isEqualTo("MH46AB1234"),
                () -> assertThat(body.carDetails().color()).isEqualTo("White"),
                () -> assertThat(body.status()).isEqualTo("PUBLISHED"),
                () -> assertThat(body.createdAt()).isNotNull()
        );

        List<Ride> rides = rideRepository.findAll();
        assertThat(rides).hasSize(1);
        Ride savedRide = rides.get(0);

        assertAll(
                () -> assertThat(savedRide.getId()).isEqualTo(body.rideId()),
                () -> assertThat(savedRide.getSource()).isEqualTo("Navi Mumbai"),
                () -> assertThat(savedRide.getDestination()).isEqualTo("Pune"),
                () -> assertThat(savedRide.getDepartureTime()).isEqualTo(Instant.parse("2026-06-15T09:30:00Z")),
                () -> assertThat(savedRide.getAvailableSeats()).isEqualTo(3),
                () -> assertThat(savedRide.getPricePerSeat()).isEqualByComparingTo("450.00"),
                () -> assertThat(savedRide.getCarMake()).isEqualTo("Maruti Suzuki"),
                () -> assertThat(savedRide.getCarModel()).isEqualTo("Ertiga"),
                () -> assertThat(savedRide.getCarNumber()).isEqualTo("MH46AB1234"),
                () -> assertThat(savedRide.getCarColor()).isEqualTo("White"),
                () -> assertThat(savedRide.getStatus()).isEqualTo(RideStatus.PUBLISHED),
                () -> assertThat(savedRide.getCreatedAt()).isNotNull()
        );
    }

    @Test
    void publishRideShouldReturnBadRequestAndNotPersistInvalidPayload() {
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

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(invalidPayload, headers);

        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/ride/publish",
                request,
                ApiErrorResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiErrorResponse body = response.getBody();
        assertThat(body).isNotNull();

        assertAll(
                () -> assertThat(body.status()).isEqualTo(400),
                () -> assertThat(body.message()).isEqualTo("Validation failed"),
                () -> assertThat(body.path()).isEqualTo("/api/v1/ride/publish"),
                () -> assertThat(body.fieldErrors()).containsKeys(
                        "source",
                        "departureTime",
                        "numberOfSeats",
                        "pricePerSeat",
                        "carDetails.make"
                )
        );
        assertThat(rideRepository.findAll()).isEmpty();
    }
}
