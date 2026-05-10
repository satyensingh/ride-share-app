# Ride Share API MVP

Java Spring Boot API for publishing driver rides. This project is Gradle-based.

## Tech Stack

- Java 17
- Spring Boot 3
- Spring Web
- Spring Data JPA
- PostgreSQL
- Docker / Docker Compose
- Gradle

## Fix for IntelliJ / Gradle Java toolchain issue

If IntelliJ showed an error like:

`Cannot find a Java installation ... matching languageVersion=21`

this project now includes the Gradle Foojay toolchain resolver in `settings.gradle`, so Gradle can auto-resolve a matching JDK 21 during import/build.

If your IDE still fails:

1. Re-import the Gradle project.
2. In IntelliJ, set **Gradle JVM** to any installed JDK 17+.
3. Let Gradle provision JDK 21 automatically, or install JDK 21 locally.

If you prefer local Java only, install JDK 21 and point IntelliJ Gradle JVM to it.

## API

### Publish Ride

`POST /api/v1/ride/publish`

Request body:

```json
{
  "source": "Navi Mumbai",
  "destination": "Pune",
  "departureTime": "2026-06-15T09:30:00Z",
  "numberOfSeats": 3,
  "pricePerSeat": 450.00,
  "carDetails": {
    "make": "Maruti Suzuki",
    "model": "Ertiga",
    "number": "MH46AB1234",
    "color": "White"
  }
}
```

Success response: `201 Created`

```json
{
  "rideId": 1,
  "source": "Navi Mumbai",
  "destination": "Pune",
  "departureTime": "2026-06-15T09:30:00Z",
  "availableSeats": 3,
  "pricePerSeat": 450.00,
  "carDetails": {
    "make": "Maruti Suzuki",
    "model": "Ertiga",
    "number": "MH46AB1234",
    "color": "White"
  },
  "status": "PUBLISHED",
  "createdAt": "2026-05-10T10:00:00Z"
}
```

## Run with Docker

```bash
docker compose up --build
```

Postgres database name: `ride-share`

App URL:

```bash
http://localhost:8080
```

## Test with curl

```bash
curl --location 'http://localhost:8080/api/v1/ride/publish' \
--header 'Content-Type: application/json' \
--data '{
  "source": "Navi Mumbai",
  "destination": "Pune",
  "departureTime": "2026-06-15T09:30:00Z",
  "numberOfSeats": 3,
  "pricePerSeat": 450.00,
  "carDetails": {
    "make": "Maruti Suzuki",
    "model": "Ertiga",
    "number": "MH46AB1234",
    "color": "White"
  }
}'
```

## Run locally without Docker app

Start only Postgres:

```bash
docker compose up postgres
```

Then run:

```bash
./gradlew bootRun
```
