package com.movieticket.booking.controller;

import com.movieticket.booking.dto.request.SeatLockRequest;
import com.movieticket.booking.dto.response.ApiResponse;
import com.movieticket.booking.dto.response.BookingResponse;
import com.movieticket.booking.dto.response.SeatResponse;
import com.movieticket.booking.security.CustomUserDetails;
import com.movieticket.booking.service.SeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @GetMapping("/showtime/{showtimeId}")
    public ResponseEntity<ApiResponse<List<SeatResponse>>> getSeats(@PathVariable Long showtimeId) {
        List<SeatResponse> seats = seatService.getSeatsForShowtime(showtimeId);
        return ResponseEntity.ok(ApiResponse.success("Seats fetched", seats));
    }

    @PostMapping("/lock")
    @PreAuthorize("hasAnyRole('CUSTOMER','STAFF','ADMIN')")
    public ResponseEntity<ApiResponse<BookingResponse>> lockSeats(
            @Valid @RequestBody SeatLockRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        BookingResponse response = seatService.lockSeats(request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Seats locked, complete payment to confirm", response));
    }
}