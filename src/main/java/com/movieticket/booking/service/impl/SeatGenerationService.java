package com.movieticket.booking.service.impl;

import com.movieticket.booking.enums.SeatStatus;
import com.movieticket.booking.model.Seat;
import com.movieticket.booking.model.Showtime;
import com.movieticket.booking.repository.SeatRepository;
import com.movieticket.booking.repository.ShowtimeRepository;
import com.movieticket.booking.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatGenerationService {

    private final SeatRepository seatRepository;
    private final ShowtimeRepository showtimeRepository;

    @Transactional
    public void generateSeatsForShowtime(Long showtimeId, int rows, int seatsPerRow) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found: " + showtimeId));

        // Delete existing seats if any
        List<Seat> existing = seatRepository.findByShowtimeId(showtimeId);
        if (!existing.isEmpty()) {
            seatRepository.deleteAll(existing);
        }

        List<Seat> seats = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            char rowLetter = (char) ('A' + r);
            for (int s = 1; s <= seatsPerRow; s++) {
                Seat seat = Seat.builder()
                        .showtime(showtime)
                        .seatNumber(rowLetter + String.valueOf(s))
                        .status(SeatStatus.AVAILABLE)
                        .build();
                seats.add(seat);
            }
        }
        seatRepository.saveAll(seats);
        log.info("Generated {} seats for showtime {}", seats.size(), showtimeId);
    }
}