package com.movieticket.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.movieticket.booking.config.JwtAuthFilter;
import com.movieticket.booking.config.SecurityConfig;
import com.movieticket.booking.dto.request.SeatLockRequest;
import com.movieticket.booking.dto.response.BookingResponse;
import com.movieticket.booking.dto.response.SeatResponse;
import com.movieticket.booking.enums.AccessType;
import com.movieticket.booking.enums.RoleType;
import com.movieticket.booking.enums.SeatStatus;
import com.movieticket.booking.exception.SeatAlreadyLockedException;
import com.movieticket.booking.model.Role;
import com.movieticket.booking.model.User;
import com.movieticket.booking.security.CustomUserDetails;
import com.movieticket.booking.security.JwtUtil;
import com.movieticket.booking.security.UserDetailsServiceImpl;
import com.movieticket.booking.service.SeatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SeatController.class)
@org.springframework.context.annotation.Import({SecurityConfig.class, JwtAuthFilter.class})
class SeatControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private SeatService seatService;
    @MockBean private UserDetailsServiceImpl userDetailsService;
    @MockBean private JwtUtil jwtUtil; // required by JwtAuthFilter's constructor

    private UsernamePasswordAuthenticationToken customerAuth() {
        Set<Role> roles = new HashSet<>();
        roles.add(Role.builder().id(1L).name(RoleType.ROLE_CUSTOMER).accessType(AccessType.EXTERNAL).build());
        User user = User.builder().id(10L).username("john").password("pw").enabled(true).roles(roles).build();
        CustomUserDetails principal = new CustomUserDetails(user);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Test
    void getSeats_returns200_publicAccess() throws Exception {
        when(seatService.getSeatsForShowtime(1L)).thenReturn(
                List.of(SeatResponse.builder().id(1L).seatNumber("A1").status(SeatStatus.AVAILABLE).build()));

        mockMvc.perform(get("/api/seats/showtime/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].seatNumber").value("A1"));
    }

    @Test
    void getSeats_returns200_withEmptyList_whenNoSeats() throws Exception {
        when(seatService.getSeatsForShowtime(2L)).thenReturn(List.of());

        mockMvc.perform(get("/api/seats/showtime/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void lockSeats_returns200_forAuthenticatedCustomer() throws Exception {
        SeatLockRequest request = SeatLockRequest.builder().showtimeId(1L).seatNumbers(List.of("A1")).build();
        when(seatService.lockSeats(any(), anyLong())).thenReturn(BookingResponse.builder().id(500L).build());

        mockMvc.perform(post("/api/seats/lock")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(customerAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(500));
    }

    @Test
    void lockSeats_returns409_whenSeatAlreadyLocked() throws Exception {
        SeatLockRequest request = SeatLockRequest.builder().showtimeId(1L).seatNumbers(List.of("A1")).build();
        when(seatService.lockSeats(any(), anyLong())).thenThrow(new SeatAlreadyLockedException("Seat A1 is currently locked"));

        mockMvc.perform(post("/api/seats/lock")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(customerAuth())))
                .andExpect(status().isConflict());
    }

    @Test
    void lockSeats_returns401_whenUnauthenticated() throws Exception {
        SeatLockRequest request = SeatLockRequest.builder().showtimeId(1L).seatNumbers(List.of("A1")).build();

        mockMvc.perform(post("/api/seats/lock")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void lockSeats_returns400_whenSeatNumbersEmpty() throws Exception {
        SeatLockRequest request = SeatLockRequest.builder().showtimeId(1L).seatNumbers(List.of()).build();

        mockMvc.perform(post("/api/seats/lock")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(customerAuth())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void lockSeats_returns400_whenShowtimeIdMissing() throws Exception {
        SeatLockRequest request = SeatLockRequest.builder().seatNumbers(List.of("A1")).build();

        mockMvc.perform(post("/api/seats/lock")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(customerAuth())))
                .andExpect(status().isBadRequest());
    }
}