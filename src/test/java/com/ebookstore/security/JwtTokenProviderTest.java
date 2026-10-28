package com.ebookstore.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtTokenProvider Unit Tests")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    // Valid Base64-encoded 256-bit secret for HS256
    private static final String TEST_SECRET =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION_MS = 3600000L; // 1 hour

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "secretKey",    TEST_SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "expirationMs", EXPIRATION_MS);
    }

    private UserDetails buildUserDetails(String email) {
        return User.builder()
                .username(email)
                .password("irrelevant")
                .authorities(Collections.emptyList())
                .build();
    }

    @Test
    @DisplayName("Should generate a non-blank JWT token")
    void shouldGenerateToken() {
        UserDetails user = buildUserDetails("user@test.com");
        String token = jwtTokenProvider.generateToken(user);
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("Should extract the correct username from a token")
    void shouldExtractUsername() {
        UserDetails user = buildUserDetails("user@test.com");
        String token = jwtTokenProvider.generateToken(user);
        assertThat(jwtTokenProvider.extractUsername(token)).isEqualTo("user@test.com");
    }

    @Test
    @DisplayName("Should validate a freshly-generated token as valid")
    void shouldValidateValidToken() {
        UserDetails user = buildUserDetails("user@test.com");
        String token = jwtTokenProvider.generateToken(user);
        assertThat(jwtTokenProvider.isTokenValid(token, user)).isTrue();
    }

    @Test
    @DisplayName("Should reject a token whose username doesn't match the UserDetails")
    void shouldRejectTokenWithWrongUser() {
        UserDetails original = buildUserDetails("user@test.com");
        UserDetails other    = buildUserDetails("other@test.com");
        String token = jwtTokenProvider.generateToken(original);
        assertThat(jwtTokenProvider.isTokenValid(token, other)).isFalse();
    }

    @Test
    @DisplayName("Should reject an expired token")
    void shouldRejectExpiredToken() {
        // Set expiration to -1 ms so token is immediately expired
        ReflectionTestUtils.setField(jwtTokenProvider, "expirationMs", -1000L);
        UserDetails user = buildUserDetails("user@test.com");
        String expiredToken = jwtTokenProvider.generateToken(user);
        assertThatThrownBy(() -> jwtTokenProvider.isTokenValid(expiredToken, user))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Should return configured expiration in milliseconds")
    void shouldReturnExpirationMs() {
        assertThat(jwtTokenProvider.getExpirationMs()).isEqualTo(EXPIRATION_MS);
    }
}
