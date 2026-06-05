package com.query.ticket.service;

import com.query.ticket.enums.NotificationType;
import com.query.ticket.enums.TicketStatus;
import com.query.ticket.model.Ticket;
import com.query.ticket.model.User;
import com.query.ticket.repository.TicketRepository;
import com.query.ticket.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlaScheduler {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    /**
     * Runs every 15 minutes.
     * Step 1 — Mark newly breached tickets.
     * Step 2 — Auto-escalate already-breached tickets.
     * Step 3 — Send at-risk warnings (within last 25% of SLA).
     */
    @Scheduled(fixedRate = 15 * 60 * 1000) // every 15 minutes
    public void runSlaCheck() {
        log.info("[SLA] Running SLA check at {}", LocalDateTime.now());
        markBreachedTickets();
        autoEscalateBreachedTickets();
    }

    // ── Step 1: Mark breached ─────────────────────────────────────────────────

    private void markBreachedTickets() {
        List<Ticket> breached = ticketRepository.findSlaBreachedTickets(LocalDateTime.now());

        for (Ticket ticket : breached) {
            ticket.setSlaBreached(true);
            ticket.setSlaBreachedAt(LocalDateTime.now());
            ticket.getAuditTrail().add(Ticket.AuditEntry.builder()
                    .action("SLA_BREACHED")
                    .performedBy("SYSTEM")
                    .performedByName("System")
                    .note("SLA deadline passed. Ticket requires immediate attention.")
                    .timestamp(LocalDateTime.now())
                    .build());

            ticketRepository.save(ticket);

            notifyManagersAndAdmins(ticket,
                    "SLA Breached",
                    "Ticket \"" + ticket.getTitle() + "\" has breached its SLA deadline. Immediate action required.",
                    NotificationType.TICKET_ESCALATED);

            log.warn("[SLA] Ticket {} breached SLA", ticket.getId());
        }

        if (!breached.isEmpty()) {
            log.info("[SLA] Marked {} tickets as SLA breached", breached.size());
        }
    }

    // ── Step 2: Auto-escalate ─────────────────────────────────────────────────

    private void autoEscalateBreachedTickets() {
        List<Ticket> toEscalate = ticketRepository.findBreachedNotEscalated();

        for (Ticket ticket : toEscalate) {
            int newLevel = ticket.getEscalationLevel() + 1;

            ticket.setStatus(TicketStatus.ESCALATED);
            ticket.setEscalationLevel(newLevel);
            ticket.setEscalatedAt(LocalDateTime.now());
            ticket.setEscalationReason("Auto-escalated due to SLA breach");

            ticket.getEscalationHistory().add(Ticket.EscalationEntry.builder()
                    .level(newLevel)
                    .escalatedBy("SYSTEM")
                    .escalatedByName("System")
                    .reason("SLA deadline breached — auto-escalated by system")
                    .autoEscalated(true)
                    .escalatedAt(LocalDateTime.now())
                    .build());

            ticket.getAuditTrail().add(Ticket.AuditEntry.builder()
                    .action("AUTO_ESCALATED")
                    .performedBy("SYSTEM")
                    .performedByName("System")
                    .note("Auto-escalated to level " + newLevel + " due to SLA breach")
                    .timestamp(LocalDateTime.now())
                    .build());

            ticketRepository.save(ticket);

            // Notify customer
            userRepository.findById(ticket.getCreatedBy()).ifPresent(customer ->
                    notificationService.notify(
                            customer.getId(),
                            NotificationType.TICKET_ESCALATED,
                            "Ticket Auto-Escalated",
                            "Your ticket \"" + ticket.getTitle() + "\" was automatically escalated due to SLA breach.",
                            ticket.getId())
            );

            // Notify managers and admins
            notifyManagersAndAdmins(ticket,
                    "Ticket Auto-Escalated",
                    "Ticket \"" + ticket.getTitle() + "\" was auto-escalated (Level " + newLevel + ") due to SLA breach.",
                    NotificationType.TICKET_ESCALATED);

            log.warn("[SLA] Ticket {} auto-escalated to level {}", ticket.getId(), newLevel);
        }

        if (!toEscalate.isEmpty()) {
            log.info("[SLA] Auto-escalated {} tickets", toEscalate.size());
        }
    }

    // ── Helper: notify all managers and admins ────────────────────────────────

    private void notifyManagersAndAdmins(Ticket ticket, String title, String message,
                                          NotificationType type) {
        List<User> recipients = userRepository.findAll().stream()
                .filter(u -> u.getRole().name().equals("MANAGER")
                        || u.getRole().name().equals("ADMIN"))
                .toList();

        for (User recipient : recipients) {
            // In-app notification
            notificationService.notify(
                    recipient.getId(), type, title, message, ticket.getId());

            // Email notification
            emailService.sendSlaBreachedEmail(
                    recipient.getEmail(), recipient.getName(),
                    ticket.getId(), ticket.getTitle(),
                    ticket.getPriority().name());
        }
    }
}