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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

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

    @MockBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void cleanDatabase() {
        rideRepository.deleteAll();
    }

    @Test
    void publishRideShouldPersistRideAndReturnCreatedResponse() {
        when(jwtDecoder.decode("owner-token")).thenReturn(jwt("owner-token", "owner1", "car_owner"));

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
                authorizedJsonRequest(request, "owner-token"),
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
    void publishRideShouldAllowAdminRole() {
        when(jwtDecoder.decode("admin-token")).thenReturn(jwt("admin-token", "admin1", "admin"));

        ResponseEntity<PublishRideResponse> response = restTemplate.postForEntity(
                "/api/v1/ride/publish",
                authorizedJsonRequest(validPublishRideRequest(), "admin-token"),
                PublishRideResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        PublishRideResponse body = response.getBody();
        assertThat(body).isNotNull();

        assertAll(
                () -> assertThat(body.rideId()).isNotNull(),
                () -> assertThat(body.status()).isEqualTo("PUBLISHED"),
                () -> assertThat(rideRepository.findAll()).hasSize(1)
        );
    }

    @Test
    void publishRideShouldReturnBadRequestAndNotPersistInvalidPayload() {
        when(jwtDecoder.decode("owner-token")).thenReturn(jwt("owner-token", "owner1", "car_owner"));

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

        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/ride/publish",
                authorizedJsonRequest(invalidPayload, "owner-token"),
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

    @Test
    void publishRideShouldReturnUnauthorizedWithoutBearerToken() {
        PublishRideRequest request = validPublishRideRequest();

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/ride/publish",
                request,
                String.class
        );

        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED),
                () -> assertThat(rideRepository.findAll()).isEmpty()
        );
    }

    @Test
    void publishRideShouldReturnUnauthorizedForInvalidBearerToken() {
        when(jwtDecoder.decode("invalid-token")).thenThrow(new JwtException("Invalid token"));

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/ride/publish",
                authorizedJsonRequest(validPublishRideRequest(), "invalid-token"),
                String.class
        );

        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED),
                () -> assertThat(rideRepository.findAll()).isEmpty()
        );
    }

    @Test
    void publishRideShouldReturnForbiddenForPassengerRole() {
        when(jwtDecoder.decode("passenger-token")).thenReturn(jwt("passenger-token", "passenger1", "passenger"));

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/ride/publish",
                authorizedJsonRequest(validPublishRideRequest(), "passenger-token"),
                String.class
        );

        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN),
                () -> assertThat(rideRepository.findAll()).isEmpty()
        );
    }

    private PublishRideRequest validPublishRideRequest() {
        return new PublishRideRequest(
                "Navi Mumbai",
                "Pune",
                Instant.parse("2026-06-15T09:30:00Z"),
                3,
                BigDecimal.valueOf(450.00),
                new CarDetailsRequest("Maruti Suzuki", "Ertiga", "MH46AB1234", "White")
        );
    }

    private <T> HttpEntity<T> authorizedJsonRequest(T body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }

    private Jwt jwt(String tokenValue, String subject, String role) {
        Instant issuedAt = Instant.parse("2026-05-10T10:00:00Z");
        return Jwt.withTokenValue(tokenValue)
                .header("alg", "RS256")
                .issuer("http://localhost:8081/realms/ride-share")
                .subject(subject)
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(3600))
                .claim("preferred_username", subject)
                .claim("realm_access", Map.of("roles", List.of(role)))
                .build();
    }
}
