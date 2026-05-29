package com.query.ticket.dto.request;

import com.query.ticket.enums.Role;
import lombok.Data;

@Data
public class UpdateUserRequest {
    private String name;
    private Role role;
    private boolean enabled;
    private String teamId;
}