package com.movieticket.booking.controller;

import com.movieticket.booking.dto.response.ApiResponse;
import com.movieticket.booking.model.Movie;
import com.movieticket.booking.repository.MovieRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/movies")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
public class AdminMovieController {

    private final MovieRepository movieRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<Movie>> createMovie(
            @Valid @RequestBody Movie movie) {

        Movie saved = movieRepository.save(movie);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Movie created successfully", saved));
    }
}