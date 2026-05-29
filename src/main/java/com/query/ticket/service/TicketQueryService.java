package com.query.ticket.service;

import com.query.ticket.dto.request.TicketFilterRequest;
import com.query.ticket.dto.response.TicketResponse;
import com.query.ticket.model.Ticket;
import com.query.ticket.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketQueryService {

    private final MongoTemplate mongoTemplate;
    private final TicketService ticketService;

    public Page<TicketResponse> search(
            TicketFilterRequest filter,
            Pageable pageable,
            UserPrincipal currentUser) {

        Query query = buildQuery(filter, currentUser);

        long total = mongoTemplate.count(query, Ticket.class);

        query.with(pageable);
        List<Ticket> tickets = mongoTemplate.find(query, Ticket.class);

        List<TicketResponse> responses = tickets.stream()
                .map(ticketService::toResponse)
                .toList();

        return new PageImpl<>(responses, pageable, total);
    }

    private Query buildQuery(TicketFilterRequest filter, UserPrincipal currentUser) {
        List<Criteria> criteria = new ArrayList<>();

        // Role-based scoping:
        // CUSTOMER — only their own tickets
        // AGENT, MANAGER, ADMIN — all tickets
        if (currentUser.getRole().name().equals("CUSTOMER")) {
            criteria.add(Criteria.where("createdBy").is(currentUser.getId()));
        }
        // Agents, Managers, Admins see ALL tickets — no scope restriction

        // Search by title (case-insensitive)
        if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
            criteria.add(Criteria.where("title").regex(filter.getSearch().trim(), "i"));
        }

        // Filter by status
        if (filter.getStatus() != null) {
            criteria.add(Criteria.where("status").is(filter.getStatus()));
        }

        // Filter by priority
        if (filter.getPriority() != null) {
            criteria.add(Criteria.where("priority").is(filter.getPriority()));
        }

        // Filter by assigned agent (Manager/Admin only — agent can filter their own)
        if (filter.getAssignedTo() != null && !filter.getAssignedTo().isBlank()) {
            criteria.add(Criteria.where("assignedTo").is(filter.getAssignedTo()));
        }

        // Filter by customer (Manager/Admin only)
        if (filter.getCreatedBy() != null && !filter.getCreatedBy().isBlank()) {
            criteria.add(Criteria.where("createdBy").is(filter.getCreatedBy()));
        }

        Query query = new Query();
        if (!criteria.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteria.toArray(new Criteria[0])));
        }

        return query;
    }
}