package com.movieticket.booking.repository;

import com.movieticket.booking.enums.SeatStatus;
import com.movieticket.booking.model.Movie;
import com.movieticket.booking.model.Seat;
import com.movieticket.booking.model.Showtime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class SeatRepositoryTest {

    @Autowired
    private SeatRepository seatRepository;
    @Autowired
    private ShowtimeRepository showtimeRepository;
    @Autowired
    private MovieRepository movieRepository;

    private Showtime showtime;

    @BeforeEach
    void setUp() {
        Movie movie = movieRepository.save(Movie.builder().title("Inception").durationMinutes(150).build());
        showtime = showtimeRepository.save(Showtime.builder()
                .movie(movie)
                .screenName("Screen 1")
                .startTime(LocalDateTime.now().plusHours(2))
                .price(BigDecimal.valueOf(200))
                .build());

        seatRepository.save(Seat.builder().showtime(showtime).seatNumber("A1").status(SeatStatus.AVAILABLE).build());
        seatRepository.save(Seat.builder().showtime(showtime).seatNumber("A2")
                .status(SeatStatus.LOCKED).lockedByUserId(1L)
                .lockExpiresAt(LocalDateTime.now().minusMinutes(1)) // expired
                .build());
    }

    @Test
    void findByShowtimeId_returnsAllSeatsForShowtime() {
        List<Seat> seats = seatRepository.findByShowtimeId(showtime.getId());
        assertThat(seats).hasSize(2);
    }

    @Test
    void findByShowtimeIdAndSeatNumber_returnsCorrectSeat() {
        Optional<Seat> seat = seatRepository.findByShowtimeIdAndSeatNumber(showtime.getId(), "A1");
        assertThat(seat).isPresent();
        assertThat(seat.get().getStatus()).isEqualTo(SeatStatus.AVAILABLE);
    }

    @Test
    void findByShowtimeIdAndSeatNumber_returnsEmpty_whenNotFound() {
        Optional<Seat> seat = seatRepository.findByShowtimeIdAndSeatNumber(showtime.getId(), "Z9");
        assertThat(seat).isEmpty();
    }

    @Test
    void findByShowtimeIdAndSeatNumberForUpdate_locksAndReturnsSeat() {
        Optional<Seat> seat = seatRepository.findByShowtimeIdAndSeatNumberForUpdate(showtime.getId(), "A1");
        assertThat(seat).isPresent();
        assertThat(seat.get().getSeatNumber()).isEqualTo("A1");
    }

    @Test
    void findByIdForUpdate_returnsSeatById() {
        Seat saved = seatRepository.findByShowtimeIdAndSeatNumber(showtime.getId(), "A1").orElseThrow();
        Optional<Seat> seat = seatRepository.findByIdForUpdate(saved.getId());
        assertThat(seat).isPresent();
    }

    @Test
    void findExpiredLocks_returnsOnlyExpiredLockedSeats() {
        List<Seat> expired = seatRepository.findExpiredLocks(LocalDateTime.now());
        assertThat(expired).hasSize(1);
        assertThat(expired.get(0).getSeatNumber()).isEqualTo("A2");
    }

    @Test
    void uniqueConstraint_preventsDuplicateSeatNumberPerShowtime() {
        Seat duplicate = Seat.builder().showtime(showtime).seatNumber("A1").status(SeatStatus.AVAILABLE).build();
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
            seatRepository.save(duplicate);
            seatRepository.flush();
        });
    }
}