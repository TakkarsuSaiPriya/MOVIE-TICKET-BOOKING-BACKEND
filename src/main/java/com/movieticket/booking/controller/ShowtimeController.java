package com.movieticket.booking.controller;

import com.movieticket.booking.dto.response.ApiResponse;
import com.movieticket.booking.dto.response.ShowtimeResponse;
import com.movieticket.booking.exception.ResourceNotFoundException;
import com.movieticket.booking.model.Showtime;
import com.movieticket.booking.repository.ShowtimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/showtimes")
@RequiredArgsConstructor
public class ShowtimeController {

    private final ShowtimeRepository showtimeRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ShowtimeResponse>>> getAllShowtimes() {

        List<ShowtimeResponse> response =
                showtimeRepository.findAll()
                        .stream()
                        .map(showtime -> ShowtimeResponse.builder()
                                .id(showtime.getId())
                                .movieId(showtime.getMovie().getId())
                                .movieTitle(showtime.getMovie().getTitle())
                                .screenName(showtime.getScreenName())
                                .startTime(showtime.getStartTime())
                                .price(showtime.getPrice())
                                .build())
                        .toList();

        return ResponseEntity.ok(
                ApiResponse.success("Showtimes fetched", response)
        );
    }

    @GetMapping("/{showtimeId}")
    public ResponseEntity<ApiResponse<ShowtimeResponse>> getShowtimeById(
            @PathVariable Long showtimeId) {

        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Showtime not found: " + showtimeId));

        ShowtimeResponse response =
                ShowtimeResponse.builder()
                        .id(showtime.getId())
                        .movieId(showtime.getMovie().getId())
                        .movieTitle(showtime.getMovie().getTitle())
                        .screenName(showtime.getScreenName())
                        .startTime(showtime.getStartTime())
                        .price(showtime.getPrice())
                        .build();

        return ResponseEntity.ok(
                ApiResponse.success("Showtime fetched", response)
        );
    }
}