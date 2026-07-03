package com.movieticket.booking.controller;

import com.movieticket.booking.dto.response.ApiResponse;
import com.movieticket.booking.exception.ResourceNotFoundException;
import com.movieticket.booking.enums.SeatStatus;
import com.movieticket.booking.model.Movie;
import com.movieticket.booking.model.Seat;
import com.movieticket.booking.model.Showtime;
import com.movieticket.booking.repository.MovieRepository;
import com.movieticket.booking.repository.SeatRepository;
import com.movieticket.booking.repository.ShowtimeRepository;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin/showtimes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
public class AdminShowtimeController {

    private final ShowtimeRepository showtimeRepository;
    private final MovieRepository    movieRepository;
    private final SeatRepository     seatRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<Showtime>> createShowtime(
            @RequestBody CreateShowtimeRequest request) {

        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Movie not found: " + request.getMovieId()));

        Showtime showtime = Showtime.builder()
                .movie(movie)
                .screenName(request.getScreenName())
                .startTime(request.getStartTime())
                .price(request.getPrice())
                .build();

        Showtime saved = showtimeRepository.save(showtime);

        int rows        = request.getRows() > 0        ? request.getRows()        : 8;
        int seatsPerRow = request.getSeatsPerRow() > 0 ? request.getSeatsPerRow() : 10;

        List<Seat> seats = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            char rowLetter = (char) ('A' + r);
            for (int s = 1; s <= seatsPerRow; s++) {
                seats.add(Seat.builder()
                        .showtime(saved)
                        .seatNumber(rowLetter + String.valueOf(s))
                        .status(SeatStatus.AVAILABLE)
                        .build());
            }
        }
        seatRepository.saveAll(seats);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Showtime created with " + seats.size() + " seats", saved));
    }

    @PostMapping("/{showtimeId}/generate-seats")
    public ResponseEntity<ApiResponse<String>> generateSeats(
            @PathVariable Long showtimeId,
            @RequestParam(defaultValue = "8")  int rows,
            @RequestParam(defaultValue = "10") int seatsPerRow) {

        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Showtime not found: " + showtimeId));

        List<Seat> existing = seatRepository.findByShowtimeId(showtimeId);
        if (!existing.isEmpty()) {
            seatRepository.deleteAll(existing);
        }

        List<Seat> seats = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            char rowLetter = (char) ('A' + r);
            for (int s = 1; s <= seatsPerRow; s++) {
                seats.add(Seat.builder()
                        .showtime(showtime)
                        .seatNumber(rowLetter + String.valueOf(s))
                        .status(SeatStatus.AVAILABLE)
                        .build());
            }
        }
        seatRepository.saveAll(seats);

        return ResponseEntity.ok(ApiResponse.success(
                "Generated " + seats.size() + " seats for showtime " + showtimeId, "OK"));
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateShowtimeRequest {
        @NotNull private Long movieId;
        @NotNull private String screenName;
        @NotNull private LocalDateTime startTime;
        @NotNull private BigDecimal price;
        private int rows        = 8;
        private int seatsPerRow = 10;
    }
}