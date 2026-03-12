package com.cdl.facilitybooking.exception;

public class BookingConflictException extends RuntimeException {

    public BookingConflictException(String facilityId, String startTime, String endTime) {
        super(String.format(
            "Facility '%s' is already booked between %s and %s. Please choose a different time slot.",
            facilityId, startTime, endTime
        ));
    }
}
