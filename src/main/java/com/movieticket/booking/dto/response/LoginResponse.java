package com.movieticket.booking.dto.response;

import lombok.*;
import java.util.Set;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoginResponse {
    private String token;
    private String username;
    private Set<String> roles;
    private long expiresInMs;
}