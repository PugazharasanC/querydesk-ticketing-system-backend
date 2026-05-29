package com.query.ticket.repository;

import com.query.ticket.model.Comment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends MongoRepository<Comment, String> {
    List<Comment> findByTicketIdOrderByCreatedAtAsc(String ticketId);
    List<Comment> findByTicketIdAndInternalNoteOrderByCreatedAtAsc(String ticketId, boolean internalNote);
}