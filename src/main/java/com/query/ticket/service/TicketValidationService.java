package com.query.ticket.service;

import com.query.ticket.enums.TicketStatus;
import com.query.ticket.model.Ticket;
import com.query.ticket.repository.TicketRepository;
import com.query.ticket.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketValidationService {

    private final TicketRepository ticketRepository;

    public Ticket findById(String id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
    }

    public void assertReadAccess(Ticket ticket, UserPrincipal currentUser) {
        String role = currentUser.getRole().name();
        if (role.equals("CUSTOMER") && !ticket.getCreatedBy().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied");
        }
    }

    /**
     * Closed/Resolved tickets are locked for everyone except ADMIN.
     */
    public void assertNotLocked(Ticket ticket, UserPrincipal currentUser) {
        boolean locked = ticket.getStatus() == TicketStatus.CLOSED
                || ticket.getStatus() == TicketStatus.RESOLVED;
        boolean isAdmin = currentUser.getRole().name().equals("ADMIN");
        if (locked && !isAdmin) {
            throw new RuntimeException("This ticket is " + ticket.getStatus()
                    + " and cannot be modified");
        }
    }

    /**
     * Hard lock — no one can act on a closed/resolved ticket (e.g. escalation).
     */
    public void assertNotClosed(Ticket ticket) {
        if (ticket.getStatus() == TicketStatus.CLOSED
                || ticket.getStatus() == TicketStatus.RESOLVED) {
            throw new RuntimeException("Cannot perform this action on a closed or resolved ticket");
        }
    }

    /**
     * Comments are locked on CLOSED tickets for everyone.
     */
    public void assertCommentsAllowed(Ticket ticket) {
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new RuntimeException("This ticket is closed. No further comments are allowed.");
        }
    }
}