package com.movieticket.booking.repository;

import com.movieticket.booking.model.Seat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByShowtimeId(Long showtimeId);

    Optional<Seat> findByShowtimeIdAndSeatNumber(Long showtimeId, String seatNumber);

    /**
     * PESSIMISTIC_WRITE acquires a row-level lock (SELECT ... FOR UPDATE)
     * so that concurrent transactions trying to lock/book the same seat
     * are forced to wait, preventing double booking.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id = :id")
    Optional<Seat> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.showtime.id = :showtimeId AND s.seatNumber = :seatNumber")
    Optional<Seat> findByShowtimeIdAndSeatNumberForUpdate(@Param("showtimeId") Long showtimeId,
                                                          @Param("seatNumber") String seatNumber);

    @Query("SELECT s FROM Seat s WHERE s.status = com.movieticket.booking.enums.SeatStatus.LOCKED AND s.lockExpiresAt < :now")
    List<Seat> findExpiredLocks(@Param("now") LocalDateTime now);
}