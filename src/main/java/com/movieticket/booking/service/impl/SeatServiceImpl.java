package com.movieticket.booking.service.impl;

import com.movieticket.booking.dto.request.SeatLockRequest;
import com.movieticket.booking.dto.response.BookingResponse;
import com.movieticket.booking.dto.response.SeatResponse;
import com.movieticket.booking.enums.BookingStatus;
import com.movieticket.booking.enums.SeatStatus;
import com.movieticket.booking.exception.ResourceNotFoundException;
import com.movieticket.booking.exception.SeatAlreadyLockedException;
import com.movieticket.booking.mapper.BookingMapper;
import com.movieticket.booking.mapper.SeatMapper;
import com.movieticket.booking.model.*;
import com.movieticket.booking.repository.BookingRepository;
import com.movieticket.booking.repository.SeatRepository;
import com.movieticket.booking.repository.ShowtimeRepository;
import com.movieticket.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Core concurrency-safe seat locking logic.
 *
 * lockSeats() runs in a single DB transaction. For every requested seat we acquire
 * a PESSIMISTIC_WRITE row lock (SELECT ... FOR UPDATE) via SeatRepository#findByShowtimeIdAndSeatNumberForUpdate.
 * This forces any other concurrent transaction trying to lock/book the same seat
 * to block until this transaction commits or rolls back -- eliminating the
 * double-booking race condition at the database level.
 */
@Service
@RequiredArgsConstructor
public class SeatServiceImpl implements com.movieticket.booking.service.SeatService {

    private final SeatRepository seatRepository;
    private final ShowtimeRepository showtimeRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final SeatMapper seatMapper;
    private final BookingMapper bookingMapper;

    @Value("${app.seat-lock.hold-duration-minutes:5}")
    private int holdDurationMinutes;

    @Override
    public List<SeatResponse> getSeatsForShowtime(Long showtimeId) {
        return seatRepository.findByShowtimeId(showtimeId)
                .stream()
                .map(seatMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BookingResponse lockSeats(SeatLockRequest request, Long userId) {
        Showtime showtime = showtimeRepository.findById(request.getShowtimeId())
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found: " + request.getShowtimeId()));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiry = now.plusMinutes(holdDurationMinutes);

        Set<Seat> lockedSeats = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;

        for (String seatNumber : request.getSeatNumbers()) {
            // Row-level lock: blocks concurrent transactions on this exact seat row.
            Seat seat = seatRepository.findByShowtimeIdAndSeatNumberForUpdate(request.getShowtimeId(), seatNumber)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Seat " + seatNumber + " not found for showtime " + request.getShowtimeId()));

            boolean isExpiredLock = seat.getStatus() == SeatStatus.LOCKED
                    && seat.getLockExpiresAt() != null
                    && seat.getLockExpiresAt().isBefore(now);

            if (seat.getStatus() == SeatStatus.BOOKED) {
                throw new SeatAlreadyLockedException("Seat " + seatNumber + " is already booked");
            }
            if (seat.getStatus() == SeatStatus.LOCKED && !isExpiredLock
                    && !seat.getLockedByUserId().equals(userId)) {
                throw new SeatAlreadyLockedException("Seat " + seatNumber + " is currently locked by another user");
            }

            seat.setStatus(SeatStatus.LOCKED);
            seat.setLockedByUserId(userId);
            seat.setLockExpiresAt(expiry);
            seatRepository.save(seat); // still within same transaction/lock

            lockedSeats.add(seat);
            total = total.add(showtime.getPrice());
        }

        Booking booking = Booking.builder()
                .user(user)
                .showtime(showtime)
                .seats(lockedSeats)
                .status(BookingStatus.PENDING)
                .totalAmount(total)
                .build();

        Booking saved = bookingRepository.save(booking);
        return bookingMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void releaseExpiredLocks() {
        LocalDateTime now = LocalDateTime.now();
        List<Seat> expired = seatRepository.findExpiredLocks(now);
        for (Seat seat : expired) {
            seat.setStatus(SeatStatus.AVAILABLE);
            seat.setLockedByUserId(null);
            seat.setLockExpiresAt(null);
            seatRepository.save(seat);
        }
    }
}