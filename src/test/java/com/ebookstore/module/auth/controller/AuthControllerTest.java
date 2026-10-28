package com.ebookstore.module.auth.controller;

import com.ebookstore.exception.DuplicateResourceException;
import com.ebookstore.exception.GlobalExceptionHandler;
import com.ebookstore.module.auth.dto.*;
import com.ebookstore.module.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("AuthController Integration Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    private static final String BASE_URL = "/v1/auth";

    // ===== Register Tests =====

    @Test
    @DisplayName("POST /register: should return 201 with auth tokens on valid input")
    void register_shouldReturn201OnValidInput() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("John").lastName("Doe")
                .email("john.doe@example.com")
                .password("Secur3@Pass")
                .build();

        AuthResponse response = buildAuthResponse();

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post(BASE_URL + "/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.access_token").value("test.jwt.token"))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /register: should return 400 when request body is invalid")
    void register_shouldReturn400OnInvalidBody() throws Exception {
        RegisterRequest invalid = RegisterRequest.builder()
                .firstName("").lastName("")
                .email("not-an-email").password("weak")
                .build();

        mockMvc.perform(post(BASE_URL + "/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors").exists());

        verify(authService, never()).register(any());
    }

    @Test
    @DisplayName("POST /register: should return 409 when email already exists")
    void register_shouldReturn409WhenEmailDuplicate() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("John").lastName("Doe")
                .email("existing@example.com")
                .password("Secur3@Pass")
                .build();

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new DuplicateResourceException("Email already registered"));

        mockMvc.perform(post(BASE_URL + "/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    // ===== Login Tests =====

    @Test
    @DisplayName("POST /login: should return 200 with tokens on valid credentials")
    void login_shouldReturn200OnValidCredentials() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("john.doe@example.com").password("Secur3@Pass").build();

        when(authService.login(any(LoginRequest.class))).thenReturn(buildAuthResponse());

        mockMvc.perform(post(BASE_URL + "/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("test.jwt.token"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));
    }

    @Test
    @DisplayName("POST /login: should return 400 when email is blank")
    void login_shouldReturn400WhenEmailBlank() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("").password("Secur3@Pass").build();

        mockMvc.perform(post(BASE_URL + "/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());

        verify(authService, never()).login(any());
    }

    // ===== Refresh Token Tests =====

    @Test
    @DisplayName("POST /refresh: should return 200 with new tokens on valid refresh token")
    void refresh_shouldReturn200OnValidToken() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");

        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(buildAuthResponse());

        mockMvc.perform(post(BASE_URL + "/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("test.jwt.token"));
    }

    // ===== Logout Tests =====

    @Test
    @WithMockUser(username = "john.doe@example.com")
    @DisplayName("POST /logout: should return 204 when authenticated user logs out")
    void logout_shouldReturn204WhenAuthenticated() throws Exception {
        doNothing().when(authService).logout(anyString());

        mockMvc.perform(post(BASE_URL + "/logout")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(authService).logout("john.doe@example.com");
    }

    // ===== Helpers =====

    private AuthResponse buildAuthResponse() {
        return AuthResponse.builder()
                .accessToken("test.jwt.token")
                .refreshToken("refresh-token-uuid")
                .tokenType("Bearer")
                .expiresIn(86400L)
                .userId(UUID.randomUUID())
                .email("john.doe@example.com")
                .fullName("John Doe")
                .roles(Set.of("ROLE_CUSTOMER"))
                .build();
    }
}
