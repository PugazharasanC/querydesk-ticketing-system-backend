package com.query.ticket.dto.response;

import com.query.ticket.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;       // null until email verified
    private String id;
    private String name;
    private String email;
    private Role role;
    private boolean emailVerified;
    private String message;     // user-facing message
}