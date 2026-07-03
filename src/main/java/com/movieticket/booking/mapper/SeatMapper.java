package com.movieticket.booking.mapper;

import com.movieticket.booking.dto.response.SeatResponse;
import com.movieticket.booking.model.Seat;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SeatMapper {
    SeatResponse toResponse(Seat seat);
}