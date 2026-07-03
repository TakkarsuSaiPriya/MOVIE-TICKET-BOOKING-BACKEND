package com.movieticket.booking.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SeatLockRequest {

    @NotNull(message = "Showtime id is required")
    private Long showtimeId;

    @NotEmpty(message = "At least one seat must be selected")
    private List<String> seatNumbers;
}