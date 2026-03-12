# Facility Booking API

A RESTful API built with Spring Boot 3.x for booking facilities such as meeting rooms or sports courts. Includes validation, conflict detection, and concurrency safety via pessimistic locking.

---

## Tech Stack

| Layer       | Technology                        |
|-------------|-----------------------------------|
| Framework   | Spring Boot 3.3.0                 |
| Language    | Java 21                           |
| Persistence | Spring Data JPA + H2 (in-memory)  |
| Validation  | Jakarta Bean Validation (JSR-380) |
| Boilerplate | Lombok                            |
| Build       | Maven 3.9+                        |
| Testing     | JUnit 5, Mockito, MockMvc         |
| Coverage    | JaCoCo                            |

---

## Project Structure

```
src/
├── main/
│   ├── java/com/cdl/facilitybooking/
│   │   ├── FacilityBookingApplication.java
│   │   ├── controller/BookingController.java
│   │   ├── service/BookingService.java
│   │   ├── repository/BookingRepository.java
│   │   ├── entity/Booking.java
│   │   ├── dto/
│   │   │   ├── BookingRequestDTO.java
│   │   │   └── BookingResponseDTO.java
│   │   └── exception/
│   │       ├── BookingConflictException.java
│   │       └── GlobalExceptionHandler.java
│   └── resources/application.properties
└── test/
    └── java/com/cdl/facilitybooking/
        ├── service/BookingServiceTest.java
        └── controller/BookingControllerTest.java
```

---

## Getting Started

**Prerequisites:** Java 21+, Maven 3.9+

```bash
cd cdl-facility-booking
mvn clean install -DskipTests
mvn spring-boot:run
```

The app starts on `http://localhost:8080`.

---

## API Reference

### POST `/api/bookings` — Create a Booking

**Request Body**

```json
{
  "facilityId": "room-A",
  "userId": "user-123",
  "startTime": "2026-03-15T10:00:00",
  "endTime":   "2026-03-15T11:00:00"
}
```

**Validation Rules**

| Rule | Behaviour |
|------|-----------|
| `facilityId`, `userId` | Must not be blank |
| `startTime`, `endTime` | Must not be null |
| `startTime` | Must be in the future |
| `endTime` | Must be after `startTime` |
| Duration | Cannot exceed 2 hours |
| Overlap | No two bookings for the same facility can overlap |

**Success — `201 Created`**

```json
{
  "id": 1,
  "facilityId": "room-A",
  "userId": "user-123",
  "startTime": "2026-03-15T10:00:00",
  "endTime":   "2026-03-15T11:00:00",
  "createdAt": "2026-03-12T09:00:00"
}
```

---

**cURL Example**

```bash
# Create a booking
curl -X POST http://localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "facilityId": "room-A",
    "userId": "user-123",
    "startTime": "2026-03-15T10:00:00",
    "endTime":   "2026-03-15T11:00:00"
  }'
```

---

### GET `/api/bookings?userId={userId}` — List User Bookings

Returns all bookings for the user ordered by start time. Returns `[]` if none exist.

```bash
# List bookings for a user
curl http://localhost:8080/api/bookings?userId=test
```

---

## Error Responses

```json
{
  "timestamp": "2026-03-12T09:00:00",
  "status": 409,
  "error": "Conflict",
  "message": "Facility 'room-A' is already booked between ...",
  "details": {}
}
```

| HTTP Status | Trigger |
|-------------|---------|
| `400` | Invalid fields, past startTime, endTime ≤ startTime, duration > 2 hours |
| `409` | Booking overlaps with an existing one |
| `500` | Unhandled exceptions |

---

## Concurrency Strategy

The overlap-check query uses `@Lock(LockModeType.PESSIMISTIC_WRITE)` (`SELECT ... FOR UPDATE`), combined with `@Transactional`. This ensures that when two requests arrive simultaneously for the same facility and time slot, only one can proceed — the second will wait, then detect the conflict and receive `409 Conflict`. No double-bookings are possible under concurrent load.

---

## Running Tests

```bash
mvn test                        # run all tests
mvn verify                      # run tests + generate JaCoCo coverage report
open target/site/jacoco/index.html  # view coverage report
```

Tests are split into:
- `BookingServiceTest` — unit tests with Mockito (no DB)
- `BookingControllerTest` — slice tests with MockMvc (no DB)

---

## H2 Console

While the app is running, browse the database at:

```
URL:       http://localhost:8080/h2-console
JDBC URL:  jdbc:h2:mem:bookingdb
Username:  cdl
Password:  (leave blank)
```
