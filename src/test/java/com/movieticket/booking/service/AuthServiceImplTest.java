package com.movieticket.booking.service;

import com.movieticket.booking.dto.request.LoginRequest;
import com.movieticket.booking.dto.request.RegisterRequest;
import com.movieticket.booking.dto.response.LoginResponse;
import com.movieticket.booking.enums.AccessType;
import com.movieticket.booking.enums.RoleType;
import com.movieticket.booking.exception.DuplicateResourceException;
import com.movieticket.booking.exception.InvalidCredentialsException;
import com.movieticket.booking.model.Role;
import com.movieticket.booking.model.User;
import com.movieticket.booking.repository.RoleRepository;
import com.movieticket.booking.repository.UserRepository;
import com.movieticket.booking.security.JwtUtil;
import com.movieticket.booking.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtil jwtUtil;
    @Mock private Authentication authentication;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void register_success_createsCustomerUser() {
        RegisterRequest request = RegisterRequest.builder()
                .username("john").email("john@x.com").password("secret1").build();

        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("john@x.com")).thenReturn(false);
        when(roleRepository.findByName(RoleType.ROLE_CUSTOMER)).thenReturn(
                Optional.of(Role.builder().id(1L).name(RoleType.ROLE_CUSTOMER).accessType(AccessType.EXTERNAL).build()));
        when(passwordEncoder.encode("secret1")).thenReturn("ENCODED");

        authService.register(request);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_throws_whenUsernameTaken() {
        RegisterRequest request = RegisterRequest.builder().username("john").email("j@x.com").password("secret1").build();
        when(userRepository.existsByUsername("john")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_throws_whenEmailTaken() {
        RegisterRequest request = RegisterRequest.builder().username("john").email("j@x.com").password("secret1").build();
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("j@x.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
    }

    @Test
    void register_createsNewRole_whenCustomerRoleMissing() {
        RegisterRequest request = RegisterRequest.builder().username("john").email("j@x.com").password("secret1").build();
        when(userRepository.existsByUsername("john")).thenReturn(false);
        when(userRepository.existsByEmail("j@x.com")).thenReturn(false);
        when(roleRepository.findByName(RoleType.ROLE_CUSTOMER)).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenReturn(
                Role.builder().id(2L).name(RoleType.ROLE_CUSTOMER).accessType(AccessType.EXTERNAL).build());
        when(passwordEncoder.encode(any())).thenReturn("ENC");

        authService.register(request);

        verify(roleRepository).save(any(Role.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    void login_success_returnsTokenAndRoles() {
        LoginRequest request = LoginRequest.builder().username("john").password("secret1").build();

        Role role = Role.builder().id(1L).name(RoleType.ROLE_CUSTOMER).accessType(AccessType.EXTERNAL).build();
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        User user = User.builder().id(10L).username("john").password("ENC").roles(roles).enabled(true).build();

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(any())).thenReturn("jwt-token");
        when(jwtUtil.getExpirationMs()).thenReturn(3600000L);

        LoginResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUsername()).isEqualTo("john");
        assertThat(response.getRoles()).contains("ROLE_CUSTOMER");
    }

    @Test
    void login_throws_whenAuthenticationFails() {
        LoginRequest request = LoginRequest.builder().username("john").password("wrong").build();
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void login_throws_whenUserNotFoundAfterAuth() {
        LoginRequest request = LoginRequest.builder().username("john").password("secret1").build();
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(userRepository.findByUsername("john")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }
}