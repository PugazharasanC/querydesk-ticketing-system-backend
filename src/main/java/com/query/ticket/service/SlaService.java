package com.query.ticket.service;

import com.query.ticket.enums.SlaStatus;
import com.query.ticket.enums.TicketPriority;
import com.query.ticket.model.Ticket;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class SlaService {

    /**
     * SLA hours per priority:
     * CRITICAL → 2h, HIGH → 8h, MEDIUM → 24h, LOW → 72h
     */
    public LocalDateTime calculateDeadline(TicketPriority priority) {
        long hours = switch (priority) {
            case CRITICAL -> 2;
            case HIGH -> 8;
            case MEDIUM -> 24;
            case LOW -> 72;
        };
        return LocalDateTime.now().plusHours(hours);
    }

    public long getTotalSlaHours(TicketPriority priority) {
        return switch (priority) {
            case CRITICAL -> 2;
            case HIGH -> 8;
            case MEDIUM -> 24;
            case LOW -> 72;
        };
    }

    /**
     * Compute SLA status:
     * ON_TRACK  — more than 25% time remaining
     * AT_RISK   — within last 25% of allotted time
     * BREACHED  — past deadline
     */
    public SlaStatus computeStatus(Ticket ticket) {
        if (ticket.isSlaBreached() || ticket.getSlaDeadline() == null) {
            return SlaStatus.BREACHED;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = ticket.getSlaDeadline();

        if (now.isAfter(deadline)) return SlaStatus.BREACHED;

        long totalMinutes = ChronoUnit.MINUTES.between(ticket.getCreatedAt(), deadline);
        long elapsedMinutes = ChronoUnit.MINUTES.between(ticket.getCreatedAt(), now);

        if (totalMinutes <= 0) return SlaStatus.BREACHED;

        double percentUsed = (double) elapsedMinutes / totalMinutes;

        return percentUsed >= 0.75 ? SlaStatus.AT_RISK : SlaStatus.ON_TRACK;
    }

    public long getRemainingMinutes(Ticket ticket) {
        if (ticket.getSlaDeadline() == null) return 0;
        return ChronoUnit.MINUTES.between(LocalDateTime.now(), ticket.getSlaDeadline());
    }

    public boolean isBreached(Ticket ticket) {
        if (ticket.getSlaDeadline() == null) return false;
        return LocalDateTime.now().isAfter(ticket.getSlaDeadline());
    }
}