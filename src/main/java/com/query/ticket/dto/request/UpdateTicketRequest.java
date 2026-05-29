package com.query.ticket.dto.request;

import com.query.ticket.enums.TicketPriority;
import com.query.ticket.enums.TicketStatus;
import lombok.Data;

@Data
public class UpdateTicketRequest {
    private String title;
    private String description;
    private TicketStatus status;
    private TicketPriority priority;
    private String assignedTo;
    private String teamId;
    private String note;
}