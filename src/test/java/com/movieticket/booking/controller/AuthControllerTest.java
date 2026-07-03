package com.movieticket.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.movieticket.booking.config.JwtAuthFilter;
import com.movieticket.booking.config.SecurityConfig;
import com.movieticket.booking.dto.request.LoginRequest;
import com.movieticket.booking.dto.request.RegisterRequest;
import com.movieticket.booking.dto.response.LoginResponse;
import com.movieticket.booking.exception.DuplicateResourceException;
import com.movieticket.booking.exception.InvalidCredentialsException;
import com.movieticket.booking.security.JwtUtil;
import com.movieticket.booking.security.UserDetailsServiceImpl;
import com.movieticket.booking.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@org.springframework.context.annotation.Import({SecurityConfig.class, JwtAuthFilter.class})
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AuthService authService;
    @MockBean private UserDetailsServiceImpl userDetailsService; // referenced by SecurityConfig/JwtAuthFilter
    @MockBean private JwtUtil jwtUtil; // required by JwtAuthFilter's constructor

    @Test
    void register_returns201_onSuccess() throws Exception {
        RegisterRequest request = RegisterRequest.builder().username("john").email("john@x.com").password("secret1").build();
        doNothing().when(authService).register(any());

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void register_returns400_onInvalidPayload() throws Exception {
        RegisterRequest request = RegisterRequest.builder().username("").email("bad-email").password("123").build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_returns400_whenUsernameBlank() throws Exception {
        RegisterRequest request = RegisterRequest.builder().username("").email("john@x.com").password("secret1").build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_returns400_whenPasswordTooShort() throws Exception {
        RegisterRequest request = RegisterRequest.builder().username("john").email("john@x.com").password("123").build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_returns409_onDuplicateUser() throws Exception {
        RegisterRequest request = RegisterRequest.builder().username("john").email("john@x.com").password("secret1").build();
        doThrow(new DuplicateResourceException("Username already taken")).when(authService).register(any());

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void login_returns200_withToken_onSuccess() throws Exception {
        LoginRequest request = LoginRequest.builder().username("john").password("secret1").build();
        LoginResponse loginResponse = LoginResponse.builder()
                .token("jwt-token").username("john").roles(Set.of("ROLE_CUSTOMER")).expiresInMs(3600000L).build();

        when(authService.login(any())).thenReturn(loginResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("jwt-token"))
                .andExpect(jsonPath("$.data.username").value("john"));
    }

    @Test
    void login_returns401_onInvalidCredentials() throws Exception {
        LoginRequest request = LoginRequest.builder().username("john").password("wrong").build();
        when(authService.login(any())).thenThrow(new InvalidCredentialsException("Invalid username or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_returns400_whenUsernameMissing() throws Exception {
        LoginRequest request = LoginRequest.builder().username("").password("secret1").build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}