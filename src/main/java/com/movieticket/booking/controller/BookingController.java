package com.movieticket.booking.controller;

import com.movieticket.booking.dto.request.BookingConfirmRequest;
import com.movieticket.booking.dto.response.ApiResponse;
import com.movieticket.booking.dto.response.BookingResponse;
import com.movieticket.booking.security.CustomUserDetails;
import com.movieticket.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CUSTOMER','STAFF','ADMIN')")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<BookingResponse>> confirm(
            @Valid @RequestBody BookingConfirmRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        BookingResponse response = bookingService.confirmBooking(request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Booking confirmed", response));
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancel(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        BookingResponse response = bookingService.cancelBooking(bookingId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled", response));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> myBookings(
            @AuthenticationPrincipal CustomUserDetails principal) {
        List<BookingResponse> bookings = bookingService.getUserBookings(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Bookings fetched", bookings));
    }
}