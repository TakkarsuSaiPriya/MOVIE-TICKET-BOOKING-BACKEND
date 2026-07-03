package com.movieticket.booking.controller;

import com.movieticket.booking.dto.response.ApiResponse;
import com.movieticket.booking.model.Movie;
import com.movieticket.booking.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieRepository movieRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Movie>>> getAllMovies() {

        List<Movie> movies = movieRepository.findAll();

        return ResponseEntity.ok(
                ApiResponse.success("Movies fetched", movies)
        );
    }

    @GetMapping("/{movieId}")
    public ResponseEntity<ApiResponse<Movie>> getMovieById(
            @PathVariable Long movieId) {

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException(
                        "Movie not found: " + movieId));

        return ResponseEntity.ok(
                ApiResponse.success("Movie fetched", movie)
        );
    }
}