package com.query.ticket.service;

import com.query.ticket.dto.request.CreateTicketRequest;
import com.query.ticket.dto.request.UpdateTicketRequest;
import com.query.ticket.dto.response.TicketResponse;
import com.query.ticket.enums.NotificationType;
import com.query.ticket.enums.Role;
import com.query.ticket.enums.TicketStatus;
import com.query.ticket.model.Team;
import com.query.ticket.model.Ticket;
import com.query.ticket.repository.TeamRepository;
import com.query.ticket.repository.TicketRepository;
import com.query.ticket.repository.UserRepository;
import com.query.ticket.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final TicketValidationService validationService;
    private final TicketAuditService auditService;
    private final TicketAssignmentService assignmentService;
    private final SlaService slaService;

    // ── Create ────────────────────────────────────────────────────────────────

    public TicketResponse createTicket(CreateTicketRequest request, UserPrincipal currentUser) {
        if (currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.MANAGER) {
            throw new RuntimeException("Admins and Managers cannot create tickets.");
        }

        Ticket.TicketBuilder builder = Ticket.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority())
                .status(TicketStatus.OPEN)
                .createdBy(currentUser.getId())
                .createdByName(currentUser.getName())
                .slaDeadline(slaService.calculateDeadline(request.getPriority()))
                .slaBreached(false);

        // Team is strictly optional
        if (request.getTeamId() != null && !request.getTeamId().isBlank()) {
            teamRepository.findById(request.getTeamId()).ifPresent(team -> {
                builder.teamId(team.getId());
                builder.teamName(team.getName());
            });
        }

        Ticket ticket = builder.build();
        ticket.getAuditTrail().add(auditService.entry("TICKET_CREATED", currentUser,
                "Ticket created. SLA deadline: "
                        + slaService.calculateDeadline(request.getPriority())));

        Ticket saved = ticketRepository.save(ticket);

        emailService.sendTicketCreatedEmail(currentUser.getEmail(), currentUser.getName(),
                saved.getId(), saved.getTitle());
        notificationService.notifyTicketCreated(currentUser.getId(), saved.getId(),
                saved.getTitle());

        if (saved.getTeamId() != null) {
            notifyTeamMembers(saved,
                    "New ticket assigned to your team: " + saved.getTitle());
        }

        return toResponse(saved);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public Page<TicketResponse> getTickets(Pageable pageable, UserPrincipal currentUser) {
        return switch (currentUser.getRole()) {
            case CUSTOMER -> ticketRepository
                    .findByCreatedBy(currentUser.getId(), pageable)
                    .map(this::toResponse);
            default -> ticketRepository.findAll(pageable).map(this::toResponse);
        };
    }

    public TicketResponse getTicketById(String id, UserPrincipal currentUser) {
        Ticket ticket = validationService.findById(id);
        validationService.assertReadAccess(ticket, currentUser);
        return toResponse(ticket);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public TicketResponse updateTicket(String id, UpdateTicketRequest request,
                                       UserPrincipal currentUser) {
        Ticket ticket = validationService.findById(id);
        validationService.assertNotLocked(ticket, currentUser);

        TicketStatus previousStatus = ticket.getStatus();

        switch (currentUser.getRole()) {
            case CUSTOMER -> handleCustomerUpdate(ticket, request, currentUser);
            case AGENT -> handleAgentUpdate(ticket, request, currentUser);
            case MANAGER, ADMIN -> handleManagerUpdate(ticket, request, currentUser);
        }

        Ticket saved = ticketRepository.save(ticket);

        if (request.getStatus() != null && !request.getStatus().equals(previousStatus)) {
            notifyStatusChange(saved, currentUser);
        }

        return toResponse(saved);
    }

    // ── Self-assign ───────────────────────────────────────────────────────────

    public TicketResponse takeTicket(String ticketId, UserPrincipal currentUser) {
        Ticket ticket = validationService.findById(ticketId);

        if (ticket.getAssignedTo() != null) {
            throw new RuntimeException(
                    "This ticket is already assigned to " + ticket.getAssignedToName());
        }
        if (ticket.getStatus() == TicketStatus.CLOSED
                || ticket.getStatus() == TicketStatus.RESOLVED) {
            throw new RuntimeException("Cannot take a closed or resolved ticket");
        }

        ticket.setAssignedTo(currentUser.getId());
        ticket.setAssignedToName(currentUser.getName());
        ticket.setStatus(TicketStatus.IN_PROGRESS);

        userRepository.findById(currentUser.getId()).ifPresent(agent -> {
            if (agent.getTeamId() != null) {
                teamRepository.findById(agent.getTeamId()).ifPresent(team -> {
                    ticket.setTeamId(team.getId());
                    ticket.setTeamName(team.getName());
                });
            }
        });

        ticket.getAuditTrail().add(auditService.entry("SELF_ASSIGNED", currentUser,
                currentUser.getName() + " took this ticket"));

        Ticket saved = ticketRepository.save(ticket);

        userRepository.findById(saved.getCreatedBy()).ifPresent(customer ->
                notificationService.notifyStatusChanged(customer.getId(), saved.getId(),
                        saved.getTitle(), saved.getStatus().name())
        );

        return toResponse(saved);
    }

    // ── Assign ────────────────────────────────────────────────────────────────

    public TicketResponse assignTicket(String ticketId, String agentId, String note,
                                       UserPrincipal currentUser) {
        Ticket ticket = validationService.findById(ticketId);
        validationService.assertNotClosed(ticket);

        // Manager can only assign to agents in their own team
        if (currentUser.getRole() == Role.MANAGER) {
            var manager = userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new RuntimeException("Manager not found"));
            var agent = userRepository.findById(agentId)
                    .orElseThrow(() -> new RuntimeException("Agent not found"));

            if (manager.getTeamId() == null) {
                throw new RuntimeException(
                        "You are not assigned to any team. Only Admin can assign across teams.");
            }
            if (!manager.getTeamId().equals(agent.getTeamId())) {
                throw new RuntimeException(
                        "You can only assign tickets to agents in your team.");
            }
        }

        TicketResponse response = assignmentService.assign(ticket, agentId, note, currentUser);
        notificationService.notifyTicketAssigned(agentId, ticketId,
                ticket.getTitle(), currentUser.getName());
        return response;
    }

    // ── Escalate ──────────────────────────────────────────────────────────────

    public TicketResponse escalateTicket(String id, String reason, UserPrincipal currentUser) {
        Ticket ticket = validationService.findById(id);
        validationService.assertNotClosed(ticket);

        if (ticket.getStatus() == TicketStatus.ESCALATED) {
            throw new RuntimeException("Ticket is already escalated");
        }
        if (reason == null || reason.isBlank()) {
            throw new RuntimeException("Escalation reason is required");
        }

        int newLevel = ticket.getEscalationLevel() + 1;
        ticket.setStatus(TicketStatus.ESCALATED);
        ticket.setEscalationLevel(newLevel);
        ticket.setEscalatedAt(LocalDateTime.now());
        ticket.setEscalationReason(reason);

        ticket.getEscalationHistory().add(Ticket.EscalationEntry.builder()
                .level(newLevel)
                .escalatedBy(currentUser.getId())
                .escalatedByName(currentUser.getName())
                .reason(reason)
                .autoEscalated(false)
                .escalatedAt(LocalDateTime.now())
                .build());

        ticket.getAuditTrail().add(auditService.entry("ESCALATED", currentUser,
                "Escalated to level " + newLevel + ": " + reason));

        Ticket saved = ticketRepository.save(ticket);

        userRepository.findById(saved.getCreatedBy()).ifPresent(customer -> {
            emailService.sendTicketEscalatedEmail(customer.getEmail(), customer.getName(),
                    saved.getId(), saved.getTitle());
            notificationService.notifyEscalated(customer.getId(), saved.getId(),
                    saved.getTitle());
        });

        return toResponse(saved);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    public void deleteTicket(String id) {
        ticketRepository.delete(validationService.findById(id));
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    public DashboardStats getDashboardStats(UserPrincipal currentUser) {
        return switch (currentUser.getRole()) {
            case CUSTOMER -> DashboardStats.builder()
                    .total(ticketRepository.countByCreatedBy(currentUser.getId()))
                    .open(ticketRepository.countByCreatedByAndStatus(currentUser.getId(), TicketStatus.OPEN))
                    .inProgress(ticketRepository.countByCreatedByAndStatus(currentUser.getId(), TicketStatus.IN_PROGRESS))
                    .resolved(ticketRepository.countByCreatedByAndStatus(currentUser.getId(), TicketStatus.RESOLVED))
                    .escalated(ticketRepository.countByCreatedByAndStatus(currentUser.getId(), TicketStatus.ESCALATED))
                    .closed(ticketRepository.countByCreatedByAndStatus(currentUser.getId(), TicketStatus.CLOSED))
                    .build();
            default -> DashboardStats.builder()
                    .total(ticketRepository.count())
                    .open(ticketRepository.countByStatus(TicketStatus.OPEN))
                    .inProgress(ticketRepository.countByStatus(TicketStatus.IN_PROGRESS))
                    .resolved(ticketRepository.countByStatus(TicketStatus.RESOLVED))
                    .escalated(ticketRepository.countByStatus(TicketStatus.ESCALATED))
                    .closed(ticketRepository.countByStatus(TicketStatus.CLOSED))
                    .build();
        };
    }

    // ── Private handlers ──────────────────────────────────────────────────────

    private void handleCustomerUpdate(Ticket ticket, UpdateTicketRequest request,
                                      UserPrincipal currentUser) {
        if (!ticket.getCreatedBy().equals(currentUser.getId())) {
            throw new RuntimeException("Access denied: you can only update your own tickets");
        }
        if (request.getTitle() != null) ticket.setTitle(request.getTitle());
        if (request.getDescription() != null) ticket.setDescription(request.getDescription());
        if (request.getStatus() == TicketStatus.CLOSED
                || request.getStatus() == TicketStatus.RESOLVED) {
            applyStatusChange(ticket, request.getStatus(), request.getNote(), currentUser);
        }
    }

    private void handleAgentUpdate(Ticket ticket, UpdateTicketRequest request,
                                   UserPrincipal currentUser) {
        // Use String comparison — avoids any UserPrincipal type mismatch
        String assignedTo = ticket.getAssignedTo();
        String currentId = currentUser.getId();

        log.debug("[Agent Update] ticketAssignedTo={} currentUserId={}", assignedTo, currentId);

        if (request.getStatus() != null) {
            // Null check first
            if (assignedTo == null || !assignedTo.equals(currentId)) {
                throw new RuntimeException(
                        "You can only update status on tickets assigned to you. "
                        + "This ticket is assigned to: "
                        + (ticket.getAssignedToName() != null
                                ? ticket.getAssignedToName() : "no one"));
            }
            if (ticket.getStatus() == TicketStatus.ESCALATED) {
                throw new RuntimeException(
                        "Escalated tickets can only be updated by a Manager or Admin");
            }
            if (request.getStatus() == TicketStatus.ESCALATED) {
                throw new RuntimeException("Only Managers can escalate tickets");
            }
            applyStatusChange(ticket, request.getStatus(), request.getNote(), currentUser);
        }

        if (request.getPriority() != null) ticket.setPriority(request.getPriority());

        if (request.getTeamId() != null && !request.getTeamId().isBlank()) {
            applyTeamChange(ticket, request.getTeamId(), currentUser);
        }
    }

    private void handleManagerUpdate(Ticket ticket, UpdateTicketRequest request,
                                     UserPrincipal currentUser) {
        if (request.getTitle() != null) ticket.setTitle(request.getTitle());
        if (request.getDescription() != null) ticket.setDescription(request.getDescription());
        if (request.getPriority() != null) {
            ticket.setPriority(request.getPriority());
            if (!ticket.isSlaBreached()) {
                ticket.setSlaDeadline(slaService.calculateDeadline(request.getPriority()));
                ticket.getAuditTrail().add(auditService.entry("SLA_UPDATED", currentUser,
                        "Priority changed — SLA deadline recalculated"));
            }
        }
        if (request.getStatus() != null) {
            applyStatusChange(ticket, request.getStatus(), request.getNote(), currentUser);
        }
        if (request.getTeamId() != null && !request.getTeamId().isBlank()) {
            applyTeamChange(ticket, request.getTeamId(), currentUser);
        }
    }

    private void applyStatusChange(Ticket ticket, TicketStatus newStatus,
                                   String note, UserPrincipal currentUser) {
        TicketStatus old = ticket.getStatus();
        ticket.setStatus(newStatus);
        if (newStatus == TicketStatus.RESOLVED) ticket.setResolvedAt(LocalDateTime.now());
        ticket.getAuditTrail().add(auditService.entry("STATUS_CHANGED", currentUser,
                "Status: " + old + " → " + newStatus
                        + (note != null && !note.isBlank() ? " | " + note : "")));
    }

    private void applyTeamChange(Ticket ticket, String teamId, UserPrincipal currentUser) {
        Optional<Team> teamOpt = teamRepository.findById(teamId);
        if (teamOpt.isEmpty()) {
            throw new RuntimeException("Team not found: " + teamId);
        }
        Team team = teamOpt.get();
        String previousTeam = ticket.getTeamName();
        ticket.setTeamId(team.getId());
        ticket.setTeamName(team.getName());
        ticket.getAuditTrail().add(auditService.entry("TEAM_CHANGED", currentUser,
                previousTeam != null
                        ? "Team changed from " + previousTeam + " to " + team.getName()
                        : "Assigned to team: " + team.getName()));

        notifyTeamMembers(ticket, "Ticket \"" + ticket.getTitle()
                + "\" has been assigned to your team: " + team.getName());
    }

    private void notifyTeamMembers(Ticket ticket, String message) {
        if (ticket.getTeamId() == null) return;
        teamRepository.findById(ticket.getTeamId()).ifPresent(team ->
                team.getMemberIds().forEach(memberId ->
                        notificationService.notify(
                                memberId,
                                NotificationType.TICKET_ASSIGNED,
                                "Team Ticket Update",
                                message,
                                ticket.getId())
                )
        );
    }

    private void notifyStatusChange(Ticket saved, UserPrincipal changedBy) {
        userRepository.findById(saved.getCreatedBy()).ifPresent(customer -> {
            if (!customer.getId().equals(changedBy.getId())) {
                emailService.sendTicketStatusChangedEmail(customer.getEmail(), customer.getName(),
                        saved.getId(), saved.getTitle(), saved.getStatus().name());
                notificationService.notifyStatusChanged(customer.getId(), saved.getId(),
                        saved.getTitle(), saved.getStatus().name());
            }
        });
    }

    public TicketResponse toResponse(Ticket ticket) {
        return TicketResponse.builder()
                .id(ticket.getId())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .status(ticket.getStatus())
                .priority(ticket.getPriority())
                .createdBy(ticket.getCreatedBy())
                .createdByName(ticket.getCreatedByName())
                .assignedTo(ticket.getAssignedTo())
                .assignedToName(ticket.getAssignedToName())
                .teamId(ticket.getTeamId())
                .teamName(ticket.getTeamName())
                .slaDeadline(ticket.getSlaDeadline())
                .slaBreached(ticket.isSlaBreached())
                .slaBreachedAt(ticket.getSlaBreachedAt())
                .slaStatus(slaService.computeStatus(ticket))
                .slaRemainingMinutes(slaService.getRemainingMinutes(ticket))
                .escalationLevel(ticket.getEscalationLevel())
                .escalatedAt(ticket.getEscalatedAt())
                .escalationReason(ticket.getEscalationReason())
                .escalationHistory(ticket.getEscalationHistory())
                .auditTrail(ticket.getAuditTrail())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .resolvedAt(ticket.getResolvedAt())
                .build();
    }

    @lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class DashboardStats {
        private long total, open, inProgress, resolved, escalated, closed;
    }
}