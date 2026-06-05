package com.query.ticket.service;

import com.query.ticket.dto.request.LoginRequest;
import com.query.ticket.dto.request.RegisterRequest;
import com.query.ticket.dto.request.VerifyOtpRequest;
import com.query.ticket.dto.response.AuthResponse;
import com.query.ticket.model.User;
import com.query.ticket.repository.UserRepository;
import com.query.ticket.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        String otp = generateOtp();

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .enabled(true)
                .emailVerified(false)
                .verificationOtp(otp)
                .otpExpiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        userRepository.save(user);
        emailService.sendOtpEmail(user.getEmail(), user.getName(), otp);

        return AuthResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .emailVerified(false)
                .message("Registration successful. Please check your email for the OTP.")
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        // ── Step 1: Check email verified BEFORE authenticating ────────────────
        // This prevents BadCredentialsException from swallowing our custom message
        User userCheck = userRepository.findByEmail(request.getEmail()).orElse(null);

        if (userCheck != null && !userCheck.isEmailVerified()) {
            // Resend OTP automatically
            String otp = generateOtp();
            userCheck.setVerificationOtp(otp);
            userCheck.setOtpExpiresAt(LocalDateTime.now().plusMinutes(10));
            userRepository.save(userCheck);
            emailService.sendOtpEmail(userCheck.getEmail(), userCheck.getName(), otp);

            throw new RuntimeException(
                    "EMAIL_NOT_VERIFIED:Your email is not verified. "
                            + "A new OTP has been sent to " + request.getEmail());
        }

        // ── Step 2: Authenticate credentials ─────────────────────────────────
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(), request.getPassword()));

        // ── Step 3: Load user and generate token ──────────────────────────────
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtTokenProvider.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .emailVerified(true)
                .build();
    }

    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isEmailVerified()) {
            throw new RuntimeException("Email is already verified");
        }
        if (user.getVerificationOtp() == null
                || !user.getVerificationOtp().equals(request.getOtp())) {
            throw new RuntimeException("Invalid OTP. Please check your email and try again.");
        }
        if (user.getOtpExpiresAt() == null
                || LocalDateTime.now().isAfter(user.getOtpExpiresAt())) {
            throw new RuntimeException("OTP has expired. Please request a new one.");
        }

        user.setEmailVerified(true);
        user.setVerificationOtp(null);
        user.setOtpExpiresAt(null);
        userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .emailVerified(true)
                .message("Email verified successfully. Welcome to QueryDesk!")
                .build();
    }

    public AuthResponse resendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isEmailVerified()) {
            throw new RuntimeException("Email is already verified");
        }

        String otp = generateOtp();
        user.setVerificationOtp(otp);
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);
        emailService.sendOtpEmail(user.getEmail(), user.getName(), otp);

        return AuthResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .emailVerified(false)
                .message("OTP resent successfully. Please check your email.")
                .build();
    }

    private String generateOtp() {
        return String.format("%06d", new Random().nextInt(999999));
    }
}