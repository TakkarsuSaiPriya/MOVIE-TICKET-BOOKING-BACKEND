package com.movieticket.booking.service;

import com.movieticket.booking.dto.request.SeatLockRequest;
import com.movieticket.booking.dto.response.BookingResponse;
import com.movieticket.booking.dto.response.SeatResponse;

import java.util.List;

public interface SeatService {
    List<SeatResponse> getSeatsForShowtime(Long showtimeId);
    BookingResponse lockSeats(SeatLockRequest request, Long userId);
    void releaseExpiredLocks();
}