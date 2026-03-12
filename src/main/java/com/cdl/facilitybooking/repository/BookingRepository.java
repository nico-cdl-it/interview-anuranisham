package com.cdl.facilitybooking.repository;

import com.cdl.facilitybooking.entity.Booking;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Two ranges [A,B) and [C,D) overlap when A < D AND B > C.
    // PESSIMISTIC_WRITE → SELECT FOR UPDATE so concurrent transactions
    // queue up rather than racing past the overlap check.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT b FROM Booking b
             WHERE b.facilityId = :facilityId
               AND b.startTime  < :endTime
               AND b.endTime    > :startTime
            """)
    List<Booking> findOverlappingBookings(
            @Param("facilityId") String facilityId,
            @Param("startTime")  LocalDateTime startTime,
            @Param("endTime")    LocalDateTime endTime
    );

    List<Booking> findByUserIdOrderByStartTimeAsc(String userId);
}
