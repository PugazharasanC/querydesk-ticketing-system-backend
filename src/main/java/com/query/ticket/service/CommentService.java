package com.query.ticket.service;

import com.query.ticket.dto.request.AddCommentRequest;
import com.query.ticket.dto.response.CommentResponse;
import com.query.ticket.model.Comment;
import com.query.ticket.repository.CommentRepository;
//import com.query.ticket.repository.TicketRepository;
import com.query.ticket.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
//    private final TicketRepository ticketRepository;
    private final TicketValidationService validationService;

    public CommentResponse addComment(String ticketId, AddCommentRequest request, UserPrincipal currentUser) {
        var ticket = validationService.findById(ticketId);

        // No comments allowed on closed tickets — for anyone
        validationService.assertCommentsAllowed(ticket);

        // Customers cannot post internal notes
        boolean isInternal = request.isInternalNote()
                && !currentUser.getRole().name().equals("CUSTOMER");

        Comment comment = Comment.builder()
                .ticketId(ticketId)
                .content(request.getContent())
                .authorId(currentUser.getId())
                .authorName(currentUser.getName())
                .authorRole(currentUser.getRole().name())
                .internalNote(isInternal)
                .build();

        return toResponse(commentRepository.save(comment));
    }

    public List<CommentResponse> getComments(String ticketId, UserPrincipal currentUser) {
        boolean isCustomer = currentUser.getRole().name().equals("CUSTOMER");

        if (isCustomer) {
            return commentRepository
                    .findByTicketIdAndInternalNoteOrderByCreatedAtAsc(ticketId, false)
                    .stream().map(this::toResponse).toList();
        }

        return commentRepository
                .findByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream().map(this::toResponse).toList();
    }

    private CommentResponse toResponse(Comment c) {
        return CommentResponse.builder()
                .id(c.getId())
                .ticketId(c.getTicketId())
                .content(c.getContent())
                .authorId(c.getAuthorId())
                .authorName(c.getAuthorName())
                .authorRole(c.getAuthorRole())
                .internalNote(c.isInternalNote())
                .createdAt(c.getCreatedAt())
                .build();
    }
}