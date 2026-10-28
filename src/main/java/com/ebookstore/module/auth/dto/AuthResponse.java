package com.ebookstore.module.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Authentication response containing JWT tokens and user details")
public class AuthResponse {

    @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    @JsonProperty("access_token")
    private String accessToken;

    @Schema(description = "JWT refresh token", example = "550e8400-e29b-41d4-a716-446655440000")
    @JsonProperty("refresh_token")
    private String refreshToken;

    @Schema(description = "Token type", example = "Bearer")
    @JsonProperty("token_type")
    @Builder.Default
    private String tokenType = "Bearer";

    @Schema(description = "Access token expiry in seconds", example = "86400")
    @JsonProperty("expires_in")
    private long expiresIn;

    @Schema(description = "Authenticated user's ID")
    @JsonProperty("user_id")
    private UUID userId;

    @Schema(description = "Authenticated user's email", example = "john.doe@example.com")
    private String email;

    @Schema(description = "Authenticated user's full name", example = "John Doe")
    @JsonProperty("full_name")
    private String fullName;

    @Schema(description = "User's assigned roles", example = "[\"ROLE_CUSTOMER\"]")
    private Set<String> roles;
}
