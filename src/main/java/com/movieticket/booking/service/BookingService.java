package com.movieticket.booking.service;

import com.movieticket.booking.dto.request.BookingConfirmRequest;
import com.movieticket.booking.dto.response.BookingResponse;

import java.util.List;

public interface BookingService {
    BookingResponse confirmBooking(BookingConfirmRequest request, Long userId);
    BookingResponse cancelBooking(Long bookingId, Long userId);
    List<BookingResponse> getUserBookings(Long userId);
}