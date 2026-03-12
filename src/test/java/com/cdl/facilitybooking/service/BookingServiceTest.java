package com.cdl.facilitybooking.service;

import com.cdl.facilitybooking.dto.BookingRequestDTO;
import com.cdl.facilitybooking.dto.BookingResponseDTO;
import com.cdl.facilitybooking.entity.Booking;
import com.cdl.facilitybooking.exception.BookingConflictException;
import com.cdl.facilitybooking.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BookingService bookingService;

    private LocalDateTime tomorrow;

    @BeforeEach
    void setUp() {
        tomorrow = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
    }

    @Test
    void createBooking_success() {
        BookingRequestDTO request = buildRequest(tomorrow, tomorrow.plusHours(1));

        when(bookingRepository.findOverlappingBookings(any(), any(), any()))
                .thenReturn(List.of());
        when(bookingRepository.save(any(Booking.class)))
                .thenReturn(buildSavedBooking(1L, request));

        BookingResponseDTO result = bookingService.createBooking(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getFacilityId()).isEqualTo("room-A");
        assertThat(result.getUserId()).isEqualTo("user-123");

        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    void createBooking_mapsTimesCorrectly() {
        LocalDateTime start = tomorrow.plusHours(2);
        LocalDateTime end   = start.plusMinutes(90);
        BookingRequestDTO request = buildRequest(start, end);

        when(bookingRepository.findOverlappingBookings(any(), any(), any()))
                .thenReturn(List.of());
        when(bookingRepository.save(any(Booking.class)))
                .thenReturn(buildSavedBooking(5L, request));

        BookingResponseDTO result = bookingService.createBooking(request);

        assertThat(result.getStartTime()).isEqualTo(start);
        assertThat(result.getEndTime()).isEqualTo(end);
    }

    @Test
    void createBooking_conflict_throwsException() {
        BookingRequestDTO request = buildRequest(tomorrow, tomorrow.plusHours(1));

        when(bookingRepository.findOverlappingBookings(any(), any(), any()))
                .thenReturn(List.of(new Booking()));

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(BookingConflictException.class)
                .hasMessageContaining("room-A");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_startTimeInPast_throwsIllegalArgument() {
        BookingRequestDTO request = buildRequest(
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1)
        );

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("future");
    }

    @Test
    void createBooking_endBeforeStart_throwsIllegalArgument() {
        BookingRequestDTO request = buildRequest(tomorrow.plusHours(2), tomorrow.plusHours(1));

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endTime must be after startTime");
    }

    @Test
    void createBooking_endEqualsStart_throwsIllegalArgument() {
        BookingRequestDTO request = buildRequest(tomorrow, tomorrow);

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endTime must be after startTime");
    }

    @Test
    void createBooking_exceedsMaxDuration_throwsIllegalArgument() {
        BookingRequestDTO request = buildRequest(tomorrow, tomorrow.plusMinutes(121));

        assertThatThrownBy(() -> bookingService.createBooking(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2 hours");
    }

    @Test
    void createBooking_exactlyTwoHours_succeeds() {
        BookingRequestDTO request = buildRequest(tomorrow, tomorrow.plusMinutes(120));

        when(bookingRepository.findOverlappingBookings(any(), any(), any()))
                .thenReturn(List.of());
        when(bookingRepository.save(any(Booking.class)))
                .thenReturn(buildSavedBooking(2L, request));

        assertThat(bookingService.createBooking(request)).isNotNull();
    }

    @Test
    void getBookingsByUser_returnsBookings() {
        List<Booking> stored = List.of(
                buildSavedBooking(1L, buildRequest(tomorrow, tomorrow.plusHours(1))),
                buildSavedBooking(2L, buildRequest(tomorrow.plusHours(2), tomorrow.plusHours(3)))
        );

        when(bookingRepository.findByUserIdOrderByStartTimeAsc("user-123"))
                .thenReturn(stored);

        List<BookingResponseDTO> result = bookingService.getBookingsByUser("user-123");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(1).getId()).isEqualTo(2L);
    }

    @Test
    void getBookingsByUser_noBookings_returnsEmptyList() {
        when(bookingRepository.findByUserIdOrderByStartTimeAsc("unknown-user"))
                .thenReturn(List.of());

        assertThat(bookingService.getBookingsByUser("unknown-user")).isEmpty();
    }

    private BookingRequestDTO buildRequest(LocalDateTime start, LocalDateTime end) {
        BookingRequestDTO dto = new BookingRequestDTO();
        dto.setFacilityId("room-A");
        dto.setUserId("user-123");
        dto.setStartTime(start);
        dto.setEndTime(end);
        return dto;
    }

    private Booking buildSavedBooking(Long id, BookingRequestDTO req) {
        return Booking.builder()
                .id(id)
                .facilityId(req.getFacilityId())
                .userId(req.getUserId())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
