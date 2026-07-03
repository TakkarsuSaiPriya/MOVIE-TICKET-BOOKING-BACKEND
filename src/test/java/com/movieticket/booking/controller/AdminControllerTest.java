package com.movieticket.booking.controller;

import com.movieticket.booking.config.JwtAuthFilter;
import com.movieticket.booking.config.SecurityConfig;
import com.movieticket.booking.enums.AccessType;
import com.movieticket.booking.enums.RoleType;
import com.movieticket.booking.model.AuditLog;
import com.movieticket.booking.model.Role;
import com.movieticket.booking.model.User;
import com.movieticket.booking.security.CustomUserDetails;
import com.movieticket.booking.security.JwtUtil;
import com.movieticket.booking.security.UserDetailsServiceImpl;
import com.movieticket.booking.service.AuditLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies RBAC: INTERNAL (ADMIN/STAFF) vs EXTERNAL (CUSTOMER) access boundaries.
 */
@WebMvcTest(AdminController.class)
@org.springframework.context.annotation.Import({SecurityConfig.class, JwtAuthFilter.class})
class AdminControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private AuditLogService auditLogService;
    @MockBean private UserDetailsServiceImpl userDetailsService;
    @MockBean private JwtUtil jwtUtil; // required by JwtAuthFilter's constructor

    private UsernamePasswordAuthenticationToken authOf(RoleType role) {
        Set<Role> roles = new HashSet<>();
        roles.add(Role.builder().id(1L).name(role).accessType(AccessType.INTERNAL).build());
        User user = User.builder().id(1L).username("admin").password("pw").enabled(true).roles(roles).build();
        CustomUserDetails principal = new CustomUserDetails(user);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Test
    void ping_allowed_forAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/ping").with(authentication(authOf(RoleType.ROLE_ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("pong"));
    }

    @Test
    void ping_allowed_forStaff() throws Exception {
        mockMvc.perform(get("/api/admin/ping").with(authentication(authOf(RoleType.ROLE_STAFF))))
                .andExpect(status().isOk());
    }

    @Test
    void ping_forbidden_forCustomer() throws Exception {
        mockMvc.perform(get("/api/admin/ping").with(authentication(authOf(RoleType.ROLE_CUSTOMER))))
                .andExpect(status().isForbidden());
    }

    @Test
    void ping_unauthorized_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/admin/ping")).andExpect(status().isUnauthorized());
    }

    @Test
    void auditLogs_allowed_forAdminOnly() throws Exception {
        when(auditLogService.getAllLogs()).thenReturn(List.of(AuditLog.builder().id(1L).username("admin").build()));

        mockMvc.perform(get("/api/audit/logs").with(authentication(authOf(RoleType.ROLE_ADMIN))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].username").value("admin"));
    }

    @Test
    void auditLogs_forbidden_forStaff() throws Exception {
        mockMvc.perform(get("/api/audit/logs").with(authentication(authOf(RoleType.ROLE_STAFF))))
                .andExpect(status().isForbidden());
    }

    @Test
    void auditLogs_forbidden_forCustomer() throws Exception {
        mockMvc.perform(get("/api/audit/logs").with(authentication(authOf(RoleType.ROLE_CUSTOMER))))
                .andExpect(status().isForbidden());
    }

    @Test
    void auditLogs_unauthorized_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/audit/logs")).andExpect(status().isUnauthorized());
    }
}