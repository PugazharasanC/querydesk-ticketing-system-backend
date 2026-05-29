package com.query.ticket.controller;

import com.query.ticket.dto.request.AddCommentRequest;
import com.query.ticket.dto.request.CreateTicketRequest;
import com.query.ticket.dto.request.TicketFilterRequest;
import com.query.ticket.dto.request.UpdateTicketRequest;
import com.query.ticket.dto.response.CommentResponse;
import com.query.ticket.dto.response.TicketResponse;
import com.query.ticket.enums.TicketPriority;
import com.query.ticket.enums.TicketStatus;
import com.query.ticket.security.CurrentUser;
import com.query.ticket.security.UserPrincipal;
import com.query.ticket.service.CommentService;
import com.query.ticket.service.TicketQueryService;
import com.query.ticket.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Tag(name = "Tickets")
@SecurityRequirement(name = "bearerAuth")
public class TicketController {

    private final TicketService ticketService;
    private final TicketQueryService ticketQueryService;
    private final CommentService commentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'AGENT')")
    @Operation(summary = "Create ticket — Customer and Agent only")
    public ResponseEntity<TicketResponse> create(
            @Valid @RequestBody CreateTicketRequest request,
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketService.createTicket(request, currentUser));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List tickets — role scoped")
    public ResponseEntity<Page<TicketResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(ticketService.getTickets(
                PageRequest.of(page, size, Sort.by("createdAt").descending()), currentUser));
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Search and filter tickets")
    public ResponseEntity<Page<TicketResponse>> search(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) String assignedTo,
            @RequestParam(required = false) String createdBy,
            @CurrentUser UserPrincipal currentUser) {

        TicketFilterRequest filter = new TicketFilterRequest();
        filter.setSearch(search);
        filter.setStatus(status);
        filter.setPriority(priority);
        filter.setAssignedTo(assignedTo);
        filter.setCreatedBy(createdBy);

        return ResponseEntity.ok(ticketQueryService.search(
                filter, PageRequest.of(page, size, Sort.by("createdAt").descending()), currentUser));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TicketResponse> getById(
            @PathVariable String id,
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(ticketService.getTicketById(id, currentUser));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TicketResponse> update(
            @PathVariable String id,
            @RequestBody UpdateTicketRequest request,
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(ticketService.updateTicket(id, request, currentUser));
    }

    @PostMapping("/{id}/take")
    @PreAuthorize("hasRole('AGENT')")
    @Operation(summary = "Agent self-assigns an unassigned ticket")
    public ResponseEntity<TicketResponse> take(
            @PathVariable String id,
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(ticketService.takeTicket(id, currentUser));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Assign ticket to agent — Manager/Admin only")
    public ResponseEntity<TicketResponse> assign(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(ticketService.assignTicket(
                id, body.get("agentId"), body.get("note"), currentUser));
    }

    @PostMapping("/{id}/escalate")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Escalate ticket — requires reason")
    public ResponseEntity<TicketResponse> escalate(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(ticketService.escalateTicket(
                id, body.get("reason"), currentUser));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        ticketService.deleteTicket(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TicketService.DashboardStats> stats(
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(ticketService.getDashboardStats(currentUser));
    }

    // ── Comments ──────────────────────────────────────────────────────────────

    @PostMapping("/{id}/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable String id,
            @Valid @RequestBody AddCommentRequest request,
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.addComment(id, request, currentUser));
    }

    @GetMapping("/{id}/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CommentResponse>> getComments(
            @PathVariable String id,
            @CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(commentService.getComments(id, currentUser));
    }
}