package com.ebookstore.module.auth.service;

import com.ebookstore.exception.DuplicateResourceException;
import com.ebookstore.exception.InvalidTokenException;
import com.ebookstore.module.auth.dto.*;
import com.ebookstore.module.auth.mapper.AuthMapper;
import com.ebookstore.module.user.entity.RefreshToken;
import com.ebookstore.module.user.entity.Role;
import com.ebookstore.module.user.entity.User;
import com.ebookstore.module.user.repository.RefreshTokenRepository;
import com.ebookstore.module.user.repository.RoleRepository;
import com.ebookstore.module.user.repository.UserRepository;
import com.ebookstore.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock private UserRepository         userRepository;
    @Mock private RoleRepository         roleRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder        passwordEncoder;
    @Mock private JwtTokenProvider       jwtTokenProvider;
    @Mock private AuthenticationManager  authenticationManager;
    @Mock private AuthMapper             authMapper;

    @InjectMocks
    private AuthService authService;

    private static final String TEST_EMAIL    = "john.doe@example.com";
    private static final String TEST_PASSWORD = "Secur3@Pass";
    private static final String TEST_JWT      = "test.jwt.token";
    private static final String TEST_REFRESH  = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshExpirationMs", 604800000L);
    }

    // ===== Register Tests =====

    @Test
    @DisplayName("Register: should create user and return auth tokens")
    void register_shouldCreateUserAndReturnTokens() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("John").lastName("Doe")
                .email(TEST_EMAIL).password(TEST_PASSWORD).build();

        Role customerRole = Role.builder().id(1).name(Role.ROLE_CUSTOMER).build();
        User savedUser    = buildUser();

        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
        when(roleRepository.findByName(Role.ROLE_CUSTOMER)).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenProvider.generateToken(any(User.class))).thenReturn(TEST_JWT);
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(authMapper.toAuthResponse(any(User.class))).thenReturn(new AuthResponse());

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo(TEST_JWT);
        verify(userRepository).save(any(User.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Register: should throw DuplicateResourceException when email already exists")
    void register_shouldThrowWhenEmailAlreadyExists() {
        RegisterRequest request = RegisterRequest.builder()
                .email(TEST_EMAIL).password(TEST_PASSWORD)
                .firstName("John").lastName("Doe").build();

        when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining(TEST_EMAIL);

        verify(userRepository, never()).save(any());
    }

    // ===== Login Tests =====

    @Test
    @DisplayName("Login: should authenticate and return tokens on valid credentials")
    void login_shouldReturnTokensOnValidCredentials() {
        LoginRequest request = LoginRequest.builder()
                .email(TEST_EMAIL).password(TEST_PASSWORD).build();

        User user = buildUser();
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(authToken);
        when(jwtTokenProvider.generateToken(any(User.class))).thenReturn(TEST_JWT);
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(authMapper.toAuthResponse(any(User.class))).thenReturn(new AuthResponse());

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo(TEST_JWT);
        verify(authenticationManager).authenticate(any());
    }

    @Test
    @DisplayName("Login: should throw BadCredentialsException on wrong password")
    void login_shouldThrowOnBadCredentials() {
        LoginRequest request = LoginRequest.builder()
                .email(TEST_EMAIL).password("WrongPass").build();

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    // ===== Refresh Token Tests =====

    @Test
    @DisplayName("Refresh: should issue new tokens when refresh token is valid")
    void refreshToken_shouldIssueNewTokens() {
        User user = buildUser();
        RefreshToken storedToken = RefreshToken.builder()
                .id(UUID.randomUUID()).user(user).token(TEST_REFRESH)
                .expiryDate(LocalDateTime.now().plusDays(7)).revoked(false).build();

        when(refreshTokenRepository.findByToken(TEST_REFRESH)).thenReturn(Optional.of(storedToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtTokenProvider.generateToken(any(User.class))).thenReturn(TEST_JWT);
        when(jwtTokenProvider.getExpirationMs()).thenReturn(86400000L);
        when(authMapper.toAuthResponse(any(User.class))).thenReturn(new AuthResponse());

        RefreshTokenRequest request = new RefreshTokenRequest(TEST_REFRESH);
        AuthResponse response = authService.refreshToken(request);

        assertThat(response).isNotNull();
        assertThat(storedToken.isRevoked()).isTrue(); // old token revoked
    }

    @Test
    @DisplayName("Refresh: should throw InvalidTokenException when token is revoked")
    void refreshToken_shouldThrowWhenRevoked() {
        User user = buildUser();
        RefreshToken revokedToken = RefreshToken.builder()
                .id(UUID.randomUUID()).user(user).token(TEST_REFRESH)
                .expiryDate(LocalDateTime.now().plusDays(7)).revoked(true).build();

        when(refreshTokenRepository.findByToken(TEST_REFRESH)).thenReturn(Optional.of(revokedToken));

        assertThatThrownBy(() -> authService.refreshToken(new RefreshTokenRequest(TEST_REFRESH)))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("revoked");
    }

    @Test
    @DisplayName("Refresh: should throw InvalidTokenException when token is expired")
    void refreshToken_shouldThrowWhenExpired() {
        User user = buildUser();
        RefreshToken expiredToken = RefreshToken.builder()
                .id(UUID.randomUUID()).user(user).token(TEST_REFRESH)
                .expiryDate(LocalDateTime.now().minusDays(1)).revoked(false).build();

        when(refreshTokenRepository.findByToken(TEST_REFRESH)).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> authService.refreshToken(new RefreshTokenRequest(TEST_REFRESH)))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("expired");
    }

    // ===== Helpers =====

    private User buildUser() {
        Role role = Role.builder().id(1).name(Role.ROLE_CUSTOMER).build();
        return User.builder()
                .id(UUID.randomUUID())
                .firstName("John").lastName("Doe")
                .email(TEST_EMAIL)
                .passwordHash("encoded_password")
                .roles(Set.of(role))
                .build();
    }
}
