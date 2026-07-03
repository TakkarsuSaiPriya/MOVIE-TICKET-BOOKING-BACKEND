package com.movieticket.booking.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BookingConfirmRequest {

    @NotNull(message = "Booking id is required")
    private Long bookingId;

    // simulated payment token / reference
    @NotNull(message = "Payment reference is required")
    private String paymentReference;
}