package com.query.ticket.model;

import com.query.ticket.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String name;

    @Indexed(unique = true)
    private String email;

    private String password;

    private Role role;

    private String teamId;

    @Builder.Default
    private boolean enabled = true;

    // ── Email verification ────────────────────────────────────────────────────

    @Builder.Default
    private boolean emailVerified = false;

    private String verificationOtp;          // 6-digit OTP

    private LocalDateTime otpExpiresAt;      // 10 minute expiry

    // ── Timestamps ────────────────────────────────────────────────────────────

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}