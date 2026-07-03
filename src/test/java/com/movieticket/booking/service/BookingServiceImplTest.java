package com.movieticket.booking.service;

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
import com.movieticket.booking.model.Showtime;
import com.movieticket.booking.model.User;
import com.movieticket.booking.repository.BookingRepository;
import com.movieticket.booking.repository.SeatRepository;
import com.movieticket.booking.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private SeatRepository seatRepository;
    @Mock private BookingMapper bookingMapper;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private User user;
    private Booking booking;
    private Seat seat;

    @BeforeEach
    void setUp() {
        user = User.builder().id(10L).username("john").build();
        seat = Seat.builder().id(100L).seatNumber("A1")
                .status(SeatStatus.LOCKED).lockExpiresAt(LocalDateTime.now().plusMinutes(5)).build();

        Set<Seat> seats = new HashSet<>();
        seats.add(seat);

        booking = Booking.builder().id(500L).user(user)
                .showtime(Showtime.builder().id(1L).build())
                .seats(seats).status(BookingStatus.PENDING).build();
    }

    @Test
    void confirmBooking_success_whenSeatStillLocked() {
        BookingConfirmRequest request = BookingConfirmRequest.builder().bookingId(500L).paymentReference("PAY123").build();

        when(bookingRepository.findById(500L)).thenReturn(Optional.of(booking));
        when(seatRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(seat));
        when(seatRepository.save(any(Seat.class))).thenReturn(seat);
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(bookingMapper.toResponse(booking)).thenReturn(BookingResponse.builder().id(500L).status(BookingStatus.CONFIRMED).build());

        BookingResponse response = bookingService.confirmBooking(request, 10L);

        assertThat(response.getId()).isEqualTo(500L);
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.BOOKED);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void confirmBooking_throws_whenBookingNotFound() {
        BookingConfirmRequest request = BookingConfirmRequest.builder().bookingId(999L).paymentReference("X").build();
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookingService.confirmBooking(request, 10L));
    }

    @Test
    void confirmBooking_throws_whenNotOwnedByUser() {
        BookingConfirmRequest request = BookingConfirmRequest.builder().bookingId(500L).paymentReference("X").build();
        when(bookingRepository.findById(500L)).thenReturn(Optional.of(booking));

        assertThrows(UnauthorizedAccessException.class, () -> bookingService.confirmBooking(request, 999L));
    }

    @Test
    void confirmBooking_throws_whenBookingNotPending() {
        booking.setStatus(BookingStatus.CANCELLED);
        BookingConfirmRequest request = BookingConfirmRequest.builder().bookingId(500L).paymentReference("X").build();
        when(bookingRepository.findById(500L)).thenReturn(Optional.of(booking));

        assertThrows(SeatLockExpiredException.class, () -> bookingService.confirmBooking(request, 10L));
    }

    @Test
    void confirmBooking_throws_whenSeatLockExpired() {
        seat.setLockExpiresAt(LocalDateTime.now().minusMinutes(1)); // expired
        BookingConfirmRequest request = BookingConfirmRequest.builder().bookingId(500L).paymentReference("X").build();

        when(bookingRepository.findById(500L)).thenReturn(Optional.of(booking));
        when(seatRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(seat));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        assertThrows(SeatLockExpiredException.class, () -> bookingService.confirmBooking(request, 10L));
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.EXPIRED);
    }

    @Test
    void confirmBooking_throws_whenSeatStatusNotLocked() {
        seat.setStatus(SeatStatus.AVAILABLE);
        BookingConfirmRequest request = BookingConfirmRequest.builder().bookingId(500L).paymentReference("X").build();

        when(bookingRepository.findById(500L)).thenReturn(Optional.of(booking));
        when(seatRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(seat));
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        assertThrows(SeatLockExpiredException.class, () -> bookingService.confirmBooking(request, 10L));
    }

    @Test
    void confirmBooking_throws_whenSeatNotFoundDuringRelock() {
        BookingConfirmRequest request = BookingConfirmRequest.builder().bookingId(500L).paymentReference("X").build();
        when(bookingRepository.findById(500L)).thenReturn(Optional.of(booking));
        when(seatRepository.findByIdForUpdate(100L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookingService.confirmBooking(request, 10L));
    }

    @Test
    void cancelBooking_success_releasesSeatsAndSetsCancelled() {
        when(bookingRepository.findById(500L)).thenReturn(Optional.of(booking));
        when(seatRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(seat));
        when(seatRepository.save(any(Seat.class))).thenReturn(seat);
        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);
        when(bookingMapper.toResponse(booking)).thenReturn(BookingResponse.builder().id(500L).status(BookingStatus.CANCELLED).build());

        BookingResponse response = bookingService.cancelBooking(500L, 10L);

        assertThat(response.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(seat.getLockedByUserId()).isNull();
    }

    @Test
    void cancelBooking_throws_whenNotOwnedByUser() {
        when(bookingRepository.findById(500L)).thenReturn(Optional.of(booking));
        assertThrows(UnauthorizedAccessException.class, () -> bookingService.cancelBooking(500L, 999L));
    }

    @Test
    void cancelBooking_throws_whenBookingNotFound() {
        when(bookingRepository.findById(500L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> bookingService.cancelBooking(500L, 10L));
    }

    @Test
    void cancelBooking_throws_whenSeatNotFoundDuringRelease() {
        when(bookingRepository.findById(500L)).thenReturn(Optional.of(booking));
        when(seatRepository.findByIdForUpdate(100L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookingService.cancelBooking(500L, 10L));
    }

    @Test
    void getUserBookings_returnsMappedList() {
        when(bookingRepository.findByUserId(10L)).thenReturn(List.of(booking));
        when(bookingMapper.toResponse(booking)).thenReturn(BookingResponse.builder().id(500L).build());

        List<BookingResponse> result = bookingService.getUserBookings(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(500L);
    }

    @Test
    void getUserBookings_returnsEmptyList_whenNoBookings() {
        when(bookingRepository.findByUserId(10L)).thenReturn(List.of());
        assertThat(bookingService.getUserBookings(10L)).isEmpty();
    }
}