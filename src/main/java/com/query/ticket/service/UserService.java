package com.query.ticket.service;

import com.query.ticket.dto.request.AdminCreateUserRequest;
import com.query.ticket.dto.request.UpdateUserRequest;
import com.query.ticket.dto.response.UserResponse;
import com.query.ticket.model.User;
import com.query.ticket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toResponse);
    }

    public UserResponse getUserById(String id) {
        return toResponse(findById(id));
    }

    // Admin creates a user — password is optional, auto-generated if blank
    public UserResponse createUser(AdminCreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        String rawPassword = (request.getPassword() != null && !request.getPassword().isBlank())
                ? request.getPassword()
                : UUID.randomUUID().toString().substring(0, 10);

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(rawPassword))
                .role(request.getRole() != null ? request.getRole() : com.query.ticket.enums.Role.CUSTOMER)
                .enabled(true)
                .build();

        User saved = userRepository.save(user);
        emailService.sendUserCreatedEmail(saved.getEmail(), saved.getName(), rawPassword);
        return toResponse(saved);
    }

    // Admin updates name, role, enabled status, teamId
    public UserResponse updateUser(String id, UpdateUserRequest request) {
        User user = findById(id);
        com.query.ticket.enums.Role oldRole = user.getRole();
        boolean oldEnabled = user.isEnabled();

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getTeamId() != null) {
            user.setTeamId(request.getTeamId());
        }
        // Only update enabled if explicitly provided in request
        user.setEnabled(request.isEnabled());

        User saved = userRepository.save(user);

        // Notify on role change
        if (request.getRole() != null && !request.getRole().equals(oldRole)) {
            emailService.sendRoleChangedEmail(saved.getEmail(), saved.getName(),
                    saved.getRole().name());
        }

        // Notify on status change
        if (request.isEnabled() != oldEnabled) {
            emailService.sendAccountStatusEmail(saved.getEmail(), saved.getName(),
                    saved.isEnabled());
        }

        return toResponse(saved);
    }

    public void deleteUser(String id) {
        userRepository.delete(findById(id));
    }

    public UserResponse toggleUserStatus(String id) {
        User user = findById(id);
        user.setEnabled(!user.isEnabled());
        User saved = userRepository.save(user);
        emailService.sendAccountStatusEmail(saved.getEmail(), saved.getName(), saved.isEnabled());
        return toResponse(saved);
    }

    private User findById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .teamId(user.getTeamId())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}