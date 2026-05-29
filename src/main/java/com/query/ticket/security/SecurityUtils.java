package com.query.ticket.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    private SecurityUtils() {}

    public static UserPrincipal getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found");
        }
        return (UserPrincipal) auth.getPrincipal();
    }

    public static String getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public static String getCurrentUserEmail() {
        return getCurrentUser().getEmail();
    }

    public static String getCurrentUserRole() {
        return getCurrentUser().getRole().name();
    }

    public static boolean isAdmin() {
        return getCurrentUser().getRole().name().equals("ADMIN");
    }

    public static boolean isManager() {
        String role = getCurrentUser().getRole().name();
        return role.equals("MANAGER") || role.equals("ADMIN");
    }

    public static boolean isAgent() {
        String role = getCurrentUser().getRole().name();
        return role.equals("AGENT") || role.equals("MANAGER") || role.equals("ADMIN");
    }

    public static boolean isOwner(String resourceOwnerId) {
        return getCurrentUserId().equals(resourceOwnerId);
    }

    public static boolean isOwnerOrAdmin(String resourceOwnerId) {
        return isOwner(resourceOwnerId) || isAdmin();
    }
}