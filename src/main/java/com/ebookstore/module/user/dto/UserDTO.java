package com.ebookstore.module.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "User profile response")
public class UserDTO {

    @Schema(description = "User unique identifier")
    private UUID id;

    @Schema(description = "First name", example = "John")
    private String firstName;

    @Schema(description = "Last name", example = "Doe")
    private String lastName;

    @Schema(description = "Email address", example = "john.doe@example.com")
    private String email;

    @Schema(description = "Phone number", example = "+5511999999999")
    private String phone;

    @Schema(description = "Account active status")
    private boolean active;

    @Schema(description = "Assigned roles")
    private Set<String> roles;

    @Schema(description = "Account creation timestamp")
    private LocalDateTime createdAt;
}
