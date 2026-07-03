package com.movieticket.booking.repository;

import com.movieticket.booking.model.Movie;
import com.movieticket.booking.model.Showtime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ShowtimeRepositoryTest {

    @Autowired
    private ShowtimeRepository showtimeRepository;
    @Autowired
    private MovieRepository movieRepository;

    @Test
    void save_andFindById_returnsShowtimeWithMovie() {
        Movie movie = movieRepository.save(Movie.builder().title("Inception").durationMinutes(150).build());
        Showtime saved = showtimeRepository.save(Showtime.builder()
                .movie(movie).screenName("Screen 2")
                .startTime(LocalDateTime.now().plusDays(1))
                .price(BigDecimal.valueOf(180)).build());

        var found = showtimeRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getMovie().getTitle()).isEqualTo("Inception");
        assertThat(found.get().getPrice()).isEqualTo(BigDecimal.valueOf(180));
    }
}