package com.movieticket.booking.service;

import com.movieticket.booking.dto.request.SeatLockRequest;
import com.movieticket.booking.dto.response.BookingResponse;
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
import com.movieticket.booking.service.impl.SeatServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatServiceImplTest {

    @Mock private SeatRepository seatRepository;
    @Mock private ShowtimeRepository showtimeRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private UserRepository userRepository;
    @Mock private SeatMapper seatMapper;
    @Mock private BookingMapper bookingMapper;

    @InjectMocks
    private SeatServiceImpl seatService;

    private Showtime showtime;
    private User user;
    private Seat seatA1;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(seatService, "holdDurationMinutes", 5);

        showtime = Showtime.builder().id(1L).price(BigDecimal.valueOf(200)).build();
        user = User.builder().id(10L).username("john").build();
        seatA1 = Seat.builder().id(100L).showtime(showtime).seatNumber("A1").status(SeatStatus.AVAILABLE).build();
    }

    @Test
    void lockSeats_success_whenSeatAvailable() {
        SeatLockRequest request = SeatLockRequest.builder().showtimeId(1L).seatNumbers(List.of("A1")).build();

        when(showtimeRepository.findById(1L)).thenReturn(Optional.of(showtime));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(seatRepository.findByShowtimeIdAndSeatNumberForUpdate(1L, "A1")).thenReturn(Optional.of(seatA1));
        when(seatRepository.save(any(Seat.class))).thenReturn(seatA1);

        Booking savedBooking = Booking.builder().id(500L).user(user).showtime(showtime).build();
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);
        when(bookingMapper.toResponse(savedBooking)).thenReturn(BookingResponse.builder().id(500L).build());

        BookingResponse response = seatService.lockSeats(request, 10L);

        assertThat(response.getId()).isEqualTo(500L);
        assertThat(seatA1.getStatus()).isEqualTo(SeatStatus.LOCKED);
        assertThat(seatA1.getLockedByUserId()).isEqualTo(10L);
        verify(seatRepository, times(1)).save(seatA1);
    }

    @Test
    void lockSeats_locksMultipleSeats_andSumsTotalPrice() {
        Seat seatA2 = Seat.builder().id(101L).showtime(showtime).seatNumber("A2").status(SeatStatus.AVAILABLE).build();
        SeatLockRequest request = SeatLockRequest.builder().showtimeId(1L).seatNumbers(List.of("A1", "A2")).build();

        when(showtimeRepository.findById(1L)).thenReturn(Optional.of(showtime));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(seatRepository.findByShowtimeIdAndSeatNumberForUpdate(1L, "A1")).thenReturn(Optional.of(seatA1));
        when(seatRepository.findByShowtimeIdAndSeatNumberForUpdate(1L, "A2")).thenReturn(Optional.of(seatA2));
        when(seatRepository.save(any(Seat.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking savedBooking = Booking.builder().id(501L).user(user).showtime(showtime).build();
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);
        when(bookingMapper.toResponse(savedBooking)).thenReturn(BookingResponse.builder().id(501L).build());

        seatService.lockSeats(request, 10L);

        verify(bookingRepository).save(argThat(b -> b.getTotalAmount().equals(BigDecimal.valueOf(400))));
    }

    @Test
    void lockSeats_throws_whenShowtimeNotFound() {
        SeatLockRequest request = SeatLockRequest.builder().showtimeId(99L).seatNumbers(List.of("A1")).build();
        when(showtimeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> seatService.lockSeats(request, 10L));
    }

    @Test
    void lockSeats_throws_whenUserNotFound() {
        SeatLockRequest request = SeatLockRequest.builder().showtimeId(1L).seatNumbers(List.of("A1")).build();
        when(showtimeRepository.findById(1L)).thenReturn(Optional.of(showtime));
        when(userRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> seatService.lockSeats(request, 10L));
    }

    @Test
    void lockSeats_throws_whenSeatAlreadyBooked() {
        seatA1.setStatus(SeatStatus.BOOKED);
        SeatLockRequest request = SeatLockRequest.builder().showtimeId(1L).seatNumbers(List.of("A1")).build();

        when(showtimeRepository.findById(1L)).thenReturn(Optional.of(showtime));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(seatRepository.findByShowtimeIdAndSeatNumberForUpdate(1L, "A1")).thenReturn(Optional.of(seatA1));

        assertThrows(SeatAlreadyLockedException.class, () -> seatService.lockSeats(request, 10L));
    }

    @Test
    void lockSeats_throws_whenSeatLockedByAnotherUser_andNotExpired() {
        seatA1.setStatus(SeatStatus.LOCKED);
        seatA1.setLockedByUserId(999L);
        seatA1.setLockExpiresAt(LocalDateTime.now().plusMinutes(5));

        SeatLockRequest request = SeatLockRequest.builder().showtimeId(1L).seatNumbers(List.of("A1")).build();
        when(showtimeRepository.findById(1L)).thenReturn(Optional.of(showtime));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(seatRepository.findByShowtimeIdAndSeatNumberForUpdate(1L, "A1")).thenReturn(Optional.of(seatA1));

        // this is the test that proves double-booking prevention
        assertThrows(SeatAlreadyLockedException.class, () -> seatService.lockSeats(request, 10L));
    }

    @Test
    void lockSeats_succeeds_whenSameUserRelocksOwnSeat() {
        seatA1.setStatus(SeatStatus.LOCKED);
        seatA1.setLockedByUserId(10L);
        seatA1.setLockExpiresAt(LocalDateTime.now().plusMinutes(3));

        SeatLockRequest request = SeatLockRequest.builder().showtimeId(1L).seatNumbers(List.of("A1")).build();
        when(showtimeRepository.findById(1L)).thenReturn(Optional.of(showtime));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(seatRepository.findByShowtimeIdAndSeatNumberForUpdate(1L, "A1")).thenReturn(Optional.of(seatA1));
        when(seatRepository.save(any(Seat.class))).thenReturn(seatA1);

        Booking savedBooking = Booking.builder().id(502L).user(user).showtime(showtime).build();
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);
        when(bookingMapper.toResponse(savedBooking)).thenReturn(BookingResponse.builder().id(502L).build());

        BookingResponse response = seatService.lockSeats(request, 10L);
        assertThat(response.getId()).isEqualTo(502L);
    }

    @Test
    void lockSeats_succeeds_whenPreviousLockByDifferentUserHasExpired() {
        seatA1.setStatus(SeatStatus.LOCKED);
        seatA1.setLockedByUserId(999L);
        seatA1.setLockExpiresAt(LocalDateTime.now().minusMinutes(1)); // expired

        SeatLockRequest request = SeatLockRequest.builder().showtimeId(1L).seatNumbers(List.of("A1")).build();
        when(showtimeRepository.findById(1L)).thenReturn(Optional.of(showtime));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(seatRepository.findByShowtimeIdAndSeatNumberForUpdate(1L, "A1")).thenReturn(Optional.of(seatA1));
        when(seatRepository.save(any(Seat.class))).thenReturn(seatA1);

        Booking savedBooking = Booking.builder().id(501L).user(user).showtime(showtime).build();
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);
        when(bookingMapper.toResponse(savedBooking)).thenReturn(BookingResponse.builder().id(501L).build());

        BookingResponse response = seatService.lockSeats(request, 10L);

        assertThat(response.getId()).isEqualTo(501L);
        assertThat(seatA1.getLockedByUserId()).isEqualTo(10L);
    }

    @Test
    void lockSeats_throws_whenSeatNotFoundForShowtime() {
        SeatLockRequest request = SeatLockRequest.builder().showtimeId(1L).seatNumbers(List.of("Z9")).build();
        when(showtimeRepository.findById(1L)).thenReturn(Optional.of(showtime));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(seatRepository.findByShowtimeIdAndSeatNumberForUpdate(1L, "Z9")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> seatService.lockSeats(request, 10L));
    }

    @Test
    void getSeatsForShowtime_returnsMappedSeats() {
        when(seatRepository.findByShowtimeId(1L)).thenReturn(List.of(seatA1));
        when(seatMapper.toResponse(seatA1)).thenReturn(
                com.movieticket.booking.dto.response.SeatResponse.builder().id(100L).seatNumber("A1").build());

        var result = seatService.getSeatsForShowtime(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSeatNumber()).isEqualTo("A1");
    }

    @Test
    void getSeatsForShowtime_returnsEmptyList_whenNoSeats() {
        when(seatRepository.findByShowtimeId(2L)).thenReturn(List.of());
        var result = seatService.getSeatsForShowtime(2L);
        assertThat(result).isEmpty();
    }

    @Test
    void releaseExpiredLocks_resetsExpiredSeatsToAvailable() {
        seatA1.setStatus(SeatStatus.LOCKED);
        seatA1.setLockedByUserId(10L);
        seatA1.setLockExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(seatRepository.findExpiredLocks(any())).thenReturn(List.of(seatA1));

        seatService.releaseExpiredLocks();

        assertThat(seatA1.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(seatA1.getLockedByUserId()).isNull();
        verify(seatRepository).save(seatA1);
    }

    @Test
    void releaseExpiredLocks_doesNothing_whenNoExpiredLocks() {
        when(seatRepository.findExpiredLocks(any())).thenReturn(List.of());
        seatService.releaseExpiredLocks();
        verify(seatRepository, never()).save(any());
    }
}