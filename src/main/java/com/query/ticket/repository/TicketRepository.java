package com.query.ticket.repository;

import com.query.ticket.enums.TicketStatus;
import com.query.ticket.model.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepository extends MongoRepository<Ticket, String> {

    // Customer — see only their own tickets
    Page<Ticket> findByCreatedBy(String createdBy, Pageable pageable);

    // Agent — see tickets assigned to them
    Page<Ticket> findByAssignedTo(String assignedTo, Pageable pageable);

    // Filter by status
    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);
    Page<Ticket> findByCreatedByAndStatus(String createdBy, TicketStatus status, Pageable pageable);

    // Team tickets
    Page<Ticket> findByTeamId(String teamId, Pageable pageable);

    // Dashboard counts
    long countByStatus(TicketStatus status);
    long countByCreatedBy(String createdBy);
    long countByCreatedByAndStatus(String createdBy, TicketStatus status);
    long countByAssignedTo(String assignedTo);
    long countByAssignedToAndStatus(String assignedTo, TicketStatus status);
}