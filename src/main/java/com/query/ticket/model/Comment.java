package com.query.ticket.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "comments")
public class Comment {

    @Id
    private String id;

    private String ticketId;

    private String content;

    private String authorId;
    private String authorName;
    private String authorRole;

    // Internal notes are only visible to agents/managers/admins
    private boolean internalNote;

    @CreatedDate
    private LocalDateTime createdAt;
}