package com.query.ticket.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private String id;
    private String ticketId;
    private String content;
    private String authorId;
    private String authorName;
    private String authorRole;
    private boolean internalNote;
    private LocalDateTime createdAt;
}