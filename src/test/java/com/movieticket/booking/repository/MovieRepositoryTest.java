package com.movieticket.booking.repository;

import com.movieticket.booking.model.Movie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class MovieRepositoryTest {

    @Autowired
    private MovieRepository movieRepository;

    @Test
    void save_andFindById_returnsMovie() {
        Movie saved = movieRepository.save(Movie.builder().title("Oppenheimer").language("English").durationMinutes(180).build());

        var found = movieRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Oppenheimer");
    }

    @Test
    void findAll_returnsAllMovies() {
        movieRepository.save(Movie.builder().title("Dune").durationMinutes(160).build());
        movieRepository.save(Movie.builder().title("Tenet").durationMinutes(150).build());

        assertThat(movieRepository.findAll()).hasSize(2);
    }
}