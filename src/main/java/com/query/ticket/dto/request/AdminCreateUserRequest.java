package com.query.ticket.dto.request;

import com.query.ticket.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminCreateUserRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    // Optional — if not provided, system generates a temp password
    private String password;

    private Role role = Role.CUSTOMER;
}