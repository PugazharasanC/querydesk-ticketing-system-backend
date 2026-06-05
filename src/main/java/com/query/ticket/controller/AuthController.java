package com.query.ticket.controller;

import com.query.ticket.dto.request.ForgotPasswordRequest;
import com.query.ticket.dto.request.LoginRequest;
import com.query.ticket.dto.request.RegisterRequest;
import com.query.ticket.dto.request.ResetPasswordRequest;
import com.query.ticket.dto.request.VerifyOtpRequest;
import com.query.ticket.dto.response.AuthResponse;
import com.query.ticket.service.AuthService;
import com.query.ticket.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    @Operation(summary = "Register — sends OTP to email for verification")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify email with OTP — returns JWT token on success")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtp(request));
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Resend OTP to email")
    public ResponseEntity<AuthResponse> resendOtp(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(authService.resendOtp(body.get("email")));
    }

    @PostMapping("/login")
    @Operation(summary = "Login — requires verified email")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Send password reset link")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.forgotPassword(request);
        return ResponseEntity.ok(Map.of("message",
                "If an account with that email exists, a reset link has been sent."));
    }

    @GetMapping("/validate-reset-token")
    @Operation(summary = "Validate reset token")
    public ResponseEntity<Map<String, Boolean>> validateToken(@RequestParam String token) {
        return ResponseEntity.ok(Map.of("valid", passwordResetService.validateToken(token)));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using token")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password reset successfully."));
    }
}