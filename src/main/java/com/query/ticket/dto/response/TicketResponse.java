package com.query.ticket.dto.response;

import com.query.ticket.enums.SlaStatus;
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

    // Ownership
    private String createdBy;
    private String createdByName;

    // Assignment
    private String assignedTo;
    private String assignedToName;

    // Team
    private String teamId;
    private String teamName;

    // SLA
    private LocalDateTime slaDeadline;
    private boolean slaBreached;
    private LocalDateTime slaBreachedAt;
    private SlaStatus slaStatus;
    private long slaRemainingMinutes; // negative = overdue

    // Escalation
    private int escalationLevel;
    private LocalDateTime escalatedAt;
    private String escalationReason;
    private List<Ticket.EscalationEntry> escalationHistory;

    // Audit
    private List<Ticket.AuditEntry> auditTrail;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
}