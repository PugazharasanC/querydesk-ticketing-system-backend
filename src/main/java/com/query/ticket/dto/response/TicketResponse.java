package com.query.ticket.dto.response;

import com.query.ticket.enums.TicketPriority;
import com.query.ticket.enums.TicketStatus;
import com.query.ticket.model.Ticket;
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
public class TicketResponse {
    private String id;
    private String title;
    private String description;
    private TicketStatus status;
    private TicketPriority priority;
    private String createdBy;
    private String createdByName;
    private String assignedTo;
    private String assignedToName;
    private String teamId;
    private String teamName;
    private List<Ticket.AuditEntry> auditTrail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
}