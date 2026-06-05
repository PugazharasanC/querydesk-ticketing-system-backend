package com.query.ticket.repository;

import com.query.ticket.enums.TicketStatus;
import com.query.ticket.model.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TicketRepository extends MongoRepository<Ticket, String> {

    // Customer scoped
    Page<Ticket> findByCreatedBy(String createdBy, Pageable pageable);
    Page<Ticket> findByAssignedTo(String assignedTo, Pageable pageable);

    // Status filters
    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);
    Page<Ticket> findByCreatedByAndStatus(String createdBy, TicketStatus status, Pageable pageable);
    Page<Ticket> findByTeamId(String teamId, Pageable pageable);

    // Dashboard counts
    long countByStatus(TicketStatus status);
    long countByCreatedBy(String createdBy);
    long countByCreatedByAndStatus(String createdBy, TicketStatus status);
    long countByAssignedTo(String assignedTo);
    long countByAssignedToAndStatus(String assignedTo, TicketStatus status);

    // ── SLA Scheduler queries ─────────────────────────────────────────────────

    // Find active tickets whose SLA deadline has passed and not yet marked breached
    @Query("{ 'slaDeadline': { $lt: ?0 }, 'slaBreached': false, 'status': { $nin: ['RESOLVED', 'CLOSED'] } }")
    List<Ticket> findSlaBreachedTickets(LocalDateTime now);

    // Find SLA-breached tickets not yet escalated (for auto-escalation)
    @Query("{ 'slaBreached': true, 'status': { $nin: ['ESCALATED', 'RESOLVED', 'CLOSED'] } }")
    List<Ticket> findBreachedNotEscalated();

    // Find tickets at risk (within last 25% of SLA time) for warnings
    @Query("{ 'slaDeadline': { $gt: ?0, $lt: ?1 }, 'slaBreached': false, 'status': { $nin: ['RESOLVED', 'CLOSED'] } }")
    List<Ticket> findAtRiskTickets(LocalDateTime from, LocalDateTime to);
}