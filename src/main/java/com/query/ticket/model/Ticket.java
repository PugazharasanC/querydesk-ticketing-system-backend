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
import org.springframework.data.mongodb.core.index.Indexed;
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

    // Ownership
    private String createdBy;
    private String createdByName;

    // Assignment
    private String assignedTo;
    private String assignedToName;

    // Team
    private String teamId;
    private String teamName;

    // ── SLA tracking ──────────────────────────────────────────────────────────
    @Indexed
    private LocalDateTime slaDeadline;
    private boolean slaBreached;
    private LocalDateTime slaBreachedAt;

    // ── Escalation tracking ───────────────────────────────────────────────────
    @Builder.Default
    private int escalationLevel = 0;           // 0=normal, 1=escalated, 2=re-escalated
    private LocalDateTime escalatedAt;
    private String escalationReason;

    @Builder.Default
    private List<EscalationEntry> escalationHistory = new ArrayList<>();

    // ── Audit trail ───────────────────────────────────────────────────────────
    @Builder.Default
    private List<AuditEntry> auditTrail = new ArrayList<>();

    // ── Timestamps ────────────────────────────────────────────────────────────
    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime resolvedAt;

    // ── Embedded types ────────────────────────────────────────────────────────

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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EscalationEntry {
        private int level;
        private String escalatedBy;
        private String escalatedByName;
        private String reason;
        private boolean autoEscalated;
        private LocalDateTime escalatedAt;
    }
}