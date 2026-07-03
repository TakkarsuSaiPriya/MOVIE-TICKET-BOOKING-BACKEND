package com.movieticket.booking.repository;

import com.movieticket.booking.enums.BookingStatus;
import com.movieticket.booking.model.Booking;
import com.movieticket.booking.model.Movie;
import com.movieticket.booking.model.Showtime;
import com.movieticket.booking.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class BookingRepositoryTest {

    @Autowired private BookingRepository bookingRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private MovieRepository movieRepository;
    @Autowired private ShowtimeRepository showtimeRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder().username("jane").email("jane@x.com").password("pw").build());
        Movie movie = movieRepository.save(Movie.builder().title("Dune").durationMinutes(160).build());
        Showtime showtime = showtimeRepository.save(Showtime.builder()
                .movie(movie).screenName("S1").startTime(LocalDateTime.now().plusDays(1))
                .price(BigDecimal.valueOf(250)).build());

        bookingRepository.save(Booking.builder()
                .user(user).showtime(showtime).status(BookingStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(250)).build());
    }

    @Test
    void findByUserId_returnsBookingsForUser() {
        List<Booking> bookings = bookingRepository.findByUserId(user.getId());
        assertThat(bookings).hasSize(1);
        assertThat(bookings.get(0).getStatus()).isEqualTo(BookingStatus.PENDING);
    }

    @Test
    void findByUserId_returnsEmpty_whenNoBookings() {
        List<Booking> bookings = bookingRepository.findByUserId(999L);
        assertThat(bookings).isEmpty();
    }

    @Test
    void save_setsCreatedAtAndUpdatedAtTimestamps() {
        Booking booking = bookingRepository.findByUserId(user.getId()).get(0);
        assertThat(booking.getCreatedAt()).isNotNull();
        assertThat(booking.getUpdatedAt()).isNotNull();
    }
}