package com.example.LoanMatrix.service.SupportService;

import com.example.LoanMatrix.dto.Support.CreateSupportTicketRequest;
import com.example.LoanMatrix.dto.Support.SupportTicketResponse;

import java.util.List;

public interface SupportTicketService {

    SupportTicketResponse createTicket(Long customerId, CreateSupportTicketRequest request);
    List<SupportTicketResponse> getCustomerTickets(Long customerId);
    SupportTicketResponse getTicket(Long ticketId, Long customerId);
    List<SupportTicketResponse> getAllTickets();
    SupportTicketResponse updateTicketStatus(Long ticketId, String status);

    SupportTicketResponse getAdminTicket(Long ticketId);
}
