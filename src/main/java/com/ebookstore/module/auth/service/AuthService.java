package com.ebookstore.module.auth.service;

import com.ebookstore.exception.DuplicateResourceException;
import com.ebookstore.exception.InvalidTokenException;
import com.ebookstore.exception.ResourceNotFoundException;
import com.ebookstore.module.auth.dto.*;
import com.ebookstore.module.auth.mapper.AuthMapper;
import com.ebookstore.module.user.entity.RefreshToken;
import com.ebookstore.module.user.entity.Role;
import com.ebookstore.module.user.entity.User;
import com.ebookstore.module.user.repository.RefreshTokenRepository;
import com.ebookstore.module.user.repository.RoleRepository;
import com.ebookstore.module.user.repository.UserRepository;
import com.ebookstore.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository          userRepository;
    private final RoleRepository          roleRepository;
    private final RefreshTokenRepository  refreshTokenRepository;
    private final PasswordEncoder         passwordEncoder;
    private final JwtTokenProvider        jwtTokenProvider;
    private final AuthenticationManager   authenticationManager;
    private final AuthMapper              authMapper;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    // ===== Register =====

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "An account with email '" + request.getEmail() + "' already exists");
        }

        Role customerRole = roleRepository.findByName(Role.ROLE_CUSTOMER)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", Role.ROLE_CUSTOMER));

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .roles(Set.of(customerRole))
                .build();

        user = userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());

        return buildAuthResponse(user);
    }

    // ===== Login =====

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = (User) authentication.getPrincipal();
        log.info("User logged in: {}", user.getEmail());

        return buildAuthResponse(user);
    }

    // ===== Refresh Token =====

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        if (storedToken.isRevoked()) {
            throw new InvalidTokenException("Refresh token has been revoked");
        }

        if (storedToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Refresh token has expired. Please log in again");
        }

        // Rotate refresh token (revoke old, issue new)
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        User user = storedToken.getUser();
        log.info("Token refreshed for user: {}", user.getEmail());

        return buildAuthResponse(user);
    }

    // ===== Logout =====

    @Transactional
    public void logout(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        refreshTokenRepository.revokeAllUserTokens(user);
        log.info("User logged out, all tokens revoked: {}", userEmail);
    }

    // ===== Helper =====

    private AuthResponse buildAuthResponse(User user) {
        String accessToken   = jwtTokenProvider.generateToken(user);
        String refreshToken  = createAndSaveRefreshToken(user);

        AuthResponse response = authMapper.toAuthResponse(user);
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(jwtTokenProvider.getExpirationMs() / 1000);
        return response;
    }

    private String createAndSaveRefreshToken(User user) {
        // Revoke any previous active refresh tokens before issuing a new one
        refreshTokenRepository.revokeAllUserTokens(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000))
                .build();

        return refreshTokenRepository.save(refreshToken).getToken();
    }
}
