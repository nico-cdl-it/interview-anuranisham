package com.cdl.facilitybooking.controller;

import com.cdl.facilitybooking.dto.BookingRequestDTO;
import com.cdl.facilitybooking.dto.BookingResponseDTO;
import com.cdl.facilitybooking.exception.BookingConflictException;
import com.cdl.facilitybooking.exception.GlobalExceptionHandler;
import com.cdl.facilitybooking.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
@Import(GlobalExceptionHandler.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    private ObjectMapper objectMapper;
    private LocalDateTime tomorrow;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        tomorrow = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
    }

    @Test
    void createBooking_validRequest_returns201() throws Exception {
        BookingRequestDTO request   = buildRequest(tomorrow, tomorrow.plusHours(1));
        BookingResponseDTO response = buildResponse(1L, request);

        when(bookingService.createBooking(any())).thenReturn(response);

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.facilityId").value("room-A"))
                .andExpect(jsonPath("$.userId").value("user-123"));
    }

    @Test
    void createBooking_emptyBody_returns400() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void createBooking_missingFacilityId_returns400() throws Exception {
        BookingRequestDTO request = buildRequest(tomorrow, tomorrow.plusHours(1));
        request.setFacilityId(null);

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.facilityId").exists());
    }

    @Test
    void createBooking_missingUserId_returns400() throws Exception {
        BookingRequestDTO request = buildRequest(tomorrow, tomorrow.plusHours(1));
        request.setUserId(null);

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.userId").exists());
    }

    @Test
    void createBooking_conflict_returns409() throws Exception {
        BookingRequestDTO request = buildRequest(tomorrow, tomorrow.plusHours(1));

        when(bookingService.createBooking(any()))
                .thenThrow(new BookingConflictException("room-A",
                        tomorrow.toString(), tomorrow.plusHours(1).toString()));

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("room-A")));
    }

    @Test
    void createBooking_illegalArgument_returns400() throws Exception {
        BookingRequestDTO request = buildRequest(tomorrow, tomorrow.plusHours(1));

        when(bookingService.createBooking(any()))
                .thenThrow(new IllegalArgumentException("startTime must be in the future."));

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("startTime must be in the future."));
    }

    @Test
    void getBookings_withUserId_returns200() throws Exception {
        BookingRequestDTO req1 = buildRequest(tomorrow, tomorrow.plusHours(1));
        BookingRequestDTO req2 = buildRequest(tomorrow.plusHours(2), tomorrow.plusHours(3));

        when(bookingService.getBookingsByUser(eq("user-123")))
                .thenReturn(List.of(buildResponse(1L, req1), buildResponse(2L, req2)));

        mockMvc.perform(get("/api/bookings")
                        .param("userId", "user-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(2L));
    }

    @Test
    void getBookings_noBookings_returnsEmptyList() throws Exception {
        when(bookingService.getBookingsByUser(eq("ghost-user")))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/bookings")
                        .param("userId", "ghost-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getBookings_missingUserIdParam_returns400() throws Exception {
        mockMvc.perform(get("/api/bookings"))
                .andExpect(status().isBadRequest());
    }

    private BookingRequestDTO buildRequest(LocalDateTime start, LocalDateTime end) {
        BookingRequestDTO dto = new BookingRequestDTO();
        dto.setFacilityId("room-A");
        dto.setUserId("user-123");
        dto.setStartTime(start);
        dto.setEndTime(end);
        return dto;
    }

    private BookingResponseDTO buildResponse(Long id, BookingRequestDTO req) {
        return BookingResponseDTO.builder()
                .id(id)
                .facilityId(req.getFacilityId())
                .userId(req.getUserId())
                .startTime(req.getStartTime())
                .endTime(req.getEndTime())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
