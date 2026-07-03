package com.movieticket.booking.dto.response;

import com.movieticket.booking.enums.SeatStatus;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SeatResponse {
    private Long id;
    private String seatNumber;
    private SeatStatus status;
}