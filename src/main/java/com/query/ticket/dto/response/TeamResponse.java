package com.query.ticket.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamResponse {
    private String id;
    private String name;
    private String description;
    private String managerId;
    private String managerName;
    private List<String> memberIds;
    private List<String> memberNames;
    private int memberCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}