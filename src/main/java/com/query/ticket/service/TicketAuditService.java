package com.query.ticket.service;

import com.query.ticket.model.Ticket;
import com.query.ticket.security.UserPrincipal;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TicketAuditService {

    public Ticket.AuditEntry entry(String action, UserPrincipal user, String note) {
        return Ticket.AuditEntry.builder()
                .action(action)
                .performedBy(user.getId())
                .performedByName(user.getName())
                .note(note)
                .timestamp(LocalDateTime.now())
                .build();
    }
}