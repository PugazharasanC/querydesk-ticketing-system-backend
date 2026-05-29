package com.query.ticket.controller;

import com.query.ticket.dto.request.ChangePasswordRequest;
import com.query.ticket.dto.request.UpdateProfileRequest;
import com.query.ticket.dto.response.UserResponse;
import com.query.ticket.security.CurrentUser;
import com.query.ticket.security.UserPrincipal;
import com.query.ticket.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "User profile management")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<UserResponse> getProfile(@CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(profileService.getProfile(currentUser));
    }

    @PatchMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update profile name")
    public ResponseEntity<UserResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(profileService.updateProfile(request, currentUser));
    }

    @PatchMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Change password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @CurrentUser UserPrincipal currentUser) {
        profileService.changePassword(request, currentUser);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }
}