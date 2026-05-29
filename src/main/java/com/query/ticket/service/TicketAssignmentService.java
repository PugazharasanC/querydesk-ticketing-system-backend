package com.query.ticket.service;

import com.query.ticket.dto.response.TicketResponse;
import com.query.ticket.enums.TicketStatus;
import com.query.ticket.model.Ticket;
import com.query.ticket.model.User;
import com.query.ticket.repository.TeamRepository;
import com.query.ticket.repository.TicketRepository;
import com.query.ticket.repository.UserRepository;
import com.query.ticket.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketAssignmentService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final EmailService emailService;
    private final TicketAuditService auditService;

    public TicketResponse assign(Ticket ticket, String agentId, String note, UserPrincipal assignedBy) {
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent not found"));

        String agentRole = agent.getRole().name();
        if (!agentRole.equals("AGENT") && !agentRole.equals("MANAGER")) {
            throw new RuntimeException("Only agents or managers can be assigned tickets");
        }

        String previousAgent = ticket.getAssignedToName();
        ticket.setAssignedTo(agent.getId());
        ticket.setAssignedToName(agent.getName());

        // Capture team name BEFORE building audit note (avoids lambda scope issue)
        String teamName = null;
        if (agent.getTeamId() != null) {
            var teamOpt = teamRepository.findById(agent.getTeamId());
            if (teamOpt.isPresent()) {
                var team = teamOpt.get();
                ticket.setTeamId(team.getId());
                ticket.setTeamName(team.getName());
                teamName = team.getName();
            }
        }

        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }

        // Build audit note with team details — teamName is in scope here
        String teamPart = teamName != null ? " [Team: " + teamName + "]" : "";
        String auditNote = previousAgent != null
                ? "Reassigned from " + previousAgent + " to " + agent.getName() + teamPart
                : "Assigned to " + agent.getName() + teamPart;
        if (note != null && !note.isBlank()) auditNote += " | Note: " + note;

        ticket.getAuditTrail().add(auditService.entry("ASSIGNED", assignedBy, auditNote));

        Ticket saved = ticketRepository.save(ticket);

        emailService.sendTicketAssignedEmail(
                agent.getEmail(), agent.getName(),
                saved.getId(), saved.getTitle(), assignedBy.getName());

        userRepository.findById(saved.getCreatedBy()).ifPresent(customer ->
                emailService.sendTicketStatusChangedEmail(
                        customer.getEmail(), customer.getName(),
                        saved.getId(), saved.getTitle(), saved.getStatus().name())
        );

        return toResponse(saved);
    }

    private TicketResponse toResponse(Ticket ticket) {
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
                .auditTrail(ticket.getAuditTrail())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .resolvedAt(ticket.getResolvedAt())
                .build();
    }
}