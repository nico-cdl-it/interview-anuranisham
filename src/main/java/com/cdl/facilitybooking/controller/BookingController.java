package com.cdl.facilitybooking.controller;

import com.cdl.facilitybooking.dto.BookingRequestDTO;
import com.cdl.facilitybooking.dto.BookingResponseDTO;
import com.cdl.facilitybooking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponseDTO> createBooking(
            @Valid @RequestBody BookingRequestDTO request) {

        log.info("POST /api/bookings – facility={}, user={}",
                request.getFacilityId(), request.getUserId());

        BookingResponseDTO response = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<BookingResponseDTO>> getBookings(
            @RequestParam String userId) {

        log.info("GET /api/bookings – userId={}", userId);

        List<BookingResponseDTO> bookings = bookingService.getBookingsByUser(userId);
        return ResponseEntity.ok(bookings);
    }
}
