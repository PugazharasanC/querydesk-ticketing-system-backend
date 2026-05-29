package com.query.ticket.dto.request;

import com.query.ticket.enums.TicketPriority;
import com.query.ticket.enums.TicketStatus;
import lombok.Data;

@Data
public class TicketFilterRequest {
    private String search;
    private TicketStatus status;
    private TicketPriority priority;
    private String assignedTo;
    private String createdBy;
}