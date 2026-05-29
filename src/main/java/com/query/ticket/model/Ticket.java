package com.query.ticket.model;

import com.query.ticket.enums.TicketPriority;
import com.query.ticket.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tickets")
public class Ticket {

    @Id
    private String id;

    private String title;

    private String description;

    private TicketStatus status;

    private TicketPriority priority;

    // Who created the ticket (customer)
    private String createdBy;
    private String createdByName;

    // Assigned agent
    private String assignedTo;
    private String assignedToName;

    // Team assigned to
    private String teamId;
    private String teamName;

    // Audit trail entries
    @Builder.Default
    private List<AuditEntry> auditTrail = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime resolvedAt;

    // Embedded audit entry
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditEntry {
        private String action;
        private String performedBy;
        private String performedByName;
        private String note;
        private LocalDateTime timestamp;
    }
}