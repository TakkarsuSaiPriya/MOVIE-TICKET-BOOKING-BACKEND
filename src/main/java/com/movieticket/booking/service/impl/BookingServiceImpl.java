package com.movieticket.booking.service.impl;

import com.movieticket.booking.dto.request.BookingConfirmRequest;
import com.movieticket.booking.dto.response.BookingResponse;
import com.movieticket.booking.enums.BookingStatus;
import com.movieticket.booking.enums.SeatStatus;
import com.movieticket.booking.exception.ResourceNotFoundException;
import com.movieticket.booking.exception.SeatLockExpiredException;
import com.movieticket.booking.exception.UnauthorizedAccessException;
import com.movieticket.booking.mapper.BookingMapper;
import com.movieticket.booking.model.Booking;
import com.movieticket.booking.model.Seat;
import com.movieticket.booking.repository.BookingRepository;
import com.movieticket.booking.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Confirms or cancels a booking. confirmBooking re-acquires row locks on the
 * underlying seats before flipping them to BOOKED, guaranteeing that a lock
 * which expired between "lock" and "pay" is rejected rather than silently
 * double-booked.
 */
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements com.movieticket.booking.service.BookingService {

    private final BookingRepository bookingRepository;
    private final SeatRepository seatRepository;
    private final BookingMapper bookingMapper;

    @Override
    @Transactional
    public BookingResponse confirmBooking(BookingConfirmRequest request, Long userId) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + request.getBookingId()));

        if (!booking.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("This booking does not belong to the current user");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new SeatLockExpiredException("Booking is not in a confirmable state: " + booking.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();

        for (Seat seat : booking.getSeats()) {
            // re-lock the row before final commit to BOOKED
            Seat lockedSeat = seatRepository.findByIdForUpdate(seat.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Seat not found: " + seat.getId()));

            if (lockedSeat.getStatus() != SeatStatus.LOCKED
                    || lockedSeat.getLockExpiresAt() == null
                    || lockedSeat.getLockExpiresAt().isBefore(now)) {
                booking.setStatus(BookingStatus.EXPIRED);
                bookingRepository.save(booking);
                throw new SeatLockExpiredException(
                        "Seat hold for " + lockedSeat.getSeatNumber() + " has expired, please rebook");
            }

            lockedSeat.setStatus(SeatStatus.BOOKED);
            lockedSeat.setLockExpiresAt(null);
            seatRepository.save(lockedSeat);
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        Booking saved = bookingRepository.save(booking);
        return bookingMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (!booking.getUser().getId().equals(userId)) {
            throw new UnauthorizedAccessException("This booking does not belong to the current user");
        }

        for (Seat seat : booking.getSeats()) {
            Seat lockedSeat = seatRepository.findByIdForUpdate(seat.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Seat not found: " + seat.getId()));
            lockedSeat.setStatus(SeatStatus.AVAILABLE);
            lockedSeat.setLockedByUserId(null);
            lockedSeat.setLockExpiresAt(null);
            seatRepository.save(lockedSeat);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Booking saved = bookingRepository.save(booking);
        return bookingMapper.toResponse(saved);
    }

    @Override
    public List<BookingResponse> getUserBookings(Long userId) {
        return bookingRepository.findByUserId(userId)
                .stream()
                .map(bookingMapper::toResponse)
                .collect(Collectors.toList());
    }
}