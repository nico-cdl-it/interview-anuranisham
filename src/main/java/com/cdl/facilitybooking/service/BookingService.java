package com.cdl.facilitybooking.service;

import com.cdl.facilitybooking.dto.BookingRequestDTO;
import com.cdl.facilitybooking.dto.BookingResponseDTO;
import com.cdl.facilitybooking.entity.Booking;
import com.cdl.facilitybooking.exception.BookingConflictException;
import com.cdl.facilitybooking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    // max allowed booking length
    private static final int MAX_BOOKING_MINUTES = 120;

    private final BookingRepository bookingRepository;

    // overlap check + insert must be in the same tx so the PESSIMISTIC_WRITE
    // lock from the repo query is held until we commit
    @Transactional
    public BookingResponseDTO createBooking(BookingRequestDTO request) {

        log.debug("Attempting to create booking: facility={}, user={}, start={}, end={}",
                request.getFacilityId(), request.getUserId(),
                request.getStartTime(), request.getEndTime());

        validateBusinessRules(request);

        List<Booking> conflicts = bookingRepository.findOverlappingBookings(
                request.getFacilityId(),
                request.getStartTime(),
                request.getEndTime()
        );

        if (!conflicts.isEmpty()) {
            log.warn("Booking conflict detected for facility '{}' at [{} – {}]",
                    request.getFacilityId(), request.getStartTime(), request.getEndTime());
            throw new BookingConflictException(
                    request.getFacilityId(),
                    request.getStartTime().toString(),
                    request.getEndTime().toString()
            );
        }

        Booking booking = Booking.builder()
                .facilityId(request.getFacilityId())
                .userId(request.getUserId())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();

        Booking saved = bookingRepository.save(booking);
        log.info("Booking created: id={}, facility={}, user={}",
                saved.getId(), saved.getFacilityId(), saved.getUserId());

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<BookingResponseDTO> getBookingsByUser(String userId) {
        log.debug("Fetching bookings for userId={}", userId);

        return bookingRepository
                .findByUserIdOrderByStartTimeAsc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // IllegalArgumentException maps to 400 via the global handler
    private void validateBusinessRules(BookingRequestDTO request) {
        LocalDateTime now = LocalDateTime.now();

        if (!request.getStartTime().isAfter(now)) {
            throw new IllegalArgumentException(
                    "startTime must be in the future. Received: " + request.getStartTime());
        }

        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new IllegalArgumentException("endTime must be after startTime.");
        }

        long durationMinutes = Duration
                .between(request.getStartTime(), request.getEndTime())
                .toMinutes();

        if (durationMinutes > MAX_BOOKING_MINUTES) {
            throw new IllegalArgumentException(
                    "Booking duration cannot exceed 2 hours. Requested: " + durationMinutes + " minutes.");
        }
    }

    private BookingResponseDTO toResponse(Booking booking) {
        return BookingResponseDTO.builder()
                .id(booking.getId())
                .facilityId(booking.getFacilityId())
                .userId(booking.getUserId())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}
