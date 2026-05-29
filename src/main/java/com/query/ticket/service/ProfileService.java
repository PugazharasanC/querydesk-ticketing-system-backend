package com.query.ticket.service;

import com.query.ticket.dto.request.ChangePasswordRequest;
import com.query.ticket.dto.request.UpdateProfileRequest;
import com.query.ticket.dto.response.UserResponse;
import com.query.ticket.model.User;
import com.query.ticket.repository.UserRepository;
import com.query.ticket.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse getProfile(UserPrincipal currentUser) {
        User user = findUser(currentUser.getId());
        return toResponse(user);
    }

    public UserResponse updateProfile(UpdateProfileRequest request, UserPrincipal currentUser) {
        User user = findUser(currentUser.getId());
        user.setName(request.getName());
        return toResponse(userRepository.save(user));
    }

    public void changePassword(ChangePasswordRequest request, UserPrincipal currentUser) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("New passwords do not match");
        }

        User user = findUser(currentUser.getId());

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new RuntimeException("New password must be different from current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private User findUser(String id) {
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