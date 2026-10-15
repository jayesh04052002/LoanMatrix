package com.example.LoanMatrix.service.SupportService;

import com.example.LoanMatrix.dto.Support.CreateSupportTicketRequest;
import com.example.LoanMatrix.dto.Support.SupportTicketResponse;
import com.example.LoanMatrix.entity.Customer;
import com.example.LoanMatrix.entity.LoanAccount;
import com.example.LoanMatrix.entity.SupportTicket;
import com.example.LoanMatrix.exception.CustomerNotFoundException;
import com.example.LoanMatrix.exception.UnauthorizedLoanAccessException;
import com.example.LoanMatrix.repository.CustomerRepository;
import com.example.LoanMatrix.repository.LoanAccountRepository;
import com.example.LoanMatrix.repository.Support.SupportTicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupportTicketServiceImpl implements SupportTicketService {

    private final SupportTicketRepository supportTicketRepository;
    private final CustomerRepository customerRepository;
    private final LoanAccountRepository loanAccountRepository;

    @Override
    @Transactional
    public SupportTicketResponse createTicket(Long customerId, CreateSupportTicketRequest request) {

        log.info("Creating support ticket for customer ID: {}", customerId);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> {
                    log.warn("Customer not found with ID: {}", customerId);
                    return new RuntimeException("Customer not found with id: " + customerId);
                });

        LoanAccount loanAccount = null;

        if (request.getLoanAccountId() != null) {
            loanAccount = loanAccountRepository.findByIdAndCustomer_Id(
                            request.getLoanAccountId(), customerId)
                    .orElseThrow(() -> {
                        log.warn("Unauthorized loan account access for customer ID: {}", customerId);
                        return new UnauthorizedLoanAccessException(
                                "Loan account does not belong to the customer"
                        );
                    });
        }

        LocalDateTime now = LocalDateTime.now();

        SupportTicket ticket = new SupportTicket();

        ticket.setCustomer(customer);
        ticket.setLoanAccount(loanAccount);
        ticket.setSubject(request.getSubject());
        ticket.setDescription(request.getDescription());
        ticket.setStatus("OPEN");
        ticket.setCreatedAt(now);
        ticket.setUpdatedAt(now);

        SupportTicket savedTicket = supportTicketRepository.save(ticket);

        log.info("Support ticket created successfully with ID: {}", savedTicket.getId());

        return new SupportTicketResponse(
                savedTicket.getId(),
                savedTicket.getCustomer().getId(),
                savedTicket.getLoanAccount() != null ? savedTicket.getLoanAccount().getId() : null,
                savedTicket.getSubject(),
                savedTicket.getDescription(),
                savedTicket.getStatus(),
                savedTicket.getCreatedAt(),
                savedTicket.getUpdatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicketResponse> getCustomerTickets(Long customerId) {

        log.info("Fetching support tickets for customer ID: {}", customerId);

        customerRepository.findById(customerId)
                .orElseThrow(() -> {
                    log.warn("Customer not found with ID: {}", customerId);
                    return new CustomerNotFoundException(
                            "Customer not found with id: " + customerId
                    );
                });

        List<SupportTicket> tickets =
                supportTicketRepository.findByCustomer_IdOrderByCreatedAtDesc(customerId);

        log.debug("Support tickets fetched for customer: {}", tickets.size());

        List<SupportTicketResponse> responses = new ArrayList<>();

        for (SupportTicket ticket : tickets) {
            Long longAccountId = null;

            if (ticket.getLoanAccount() != null) {
                longAccountId = ticket.getLoanAccount().getId();
            }

            SupportTicketResponse response = new SupportTicketResponse(
                    ticket.getId(),
                    ticket.getCustomer().getId(),
                    longAccountId,
                    ticket.getSubject(),
                    ticket.getDescription(),
                    ticket.getStatus(),
                    ticket.getCreatedAt(),
                    ticket.getUpdatedAt()
            );

            responses.add(response);
        }

        log.info("Customer support ticket retrieval completed");

        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public SupportTicketResponse getTicket(Long ticketId, Long customerId) {

        log.info("Fetching support ticket ID: {} for customer ID: {}", ticketId, customerId);

        SupportTicket ticket = supportTicketRepository
                .findByIdAndCustomer_Id(ticketId, customerId)
                .orElseThrow(() -> {
                    log.warn("Support ticket not found for ticket ID: {}", ticketId);
                    return new RuntimeException("Support ticket not found");
                });

        Long loanAccountId = null;

        if (ticket.getLoanAccount() != null) {
            loanAccountId = ticket.getLoanAccount().getId();
        }

        log.debug("Support ticket retrieved successfully: {}", ticketId);

        return new SupportTicketResponse(
                ticket.getId(),
                ticket.getCustomer().getId(),
                loanAccountId,
                ticket.getSubject(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicketResponse> getAllTickets() {

        log.info("Fetching all support tickets");

        List<SupportTicket> tickets =
                supportTicketRepository.findAll();

        log.debug("Total support tickets fetched: {}", tickets.size());

        List<SupportTicketResponse> responses =
                new ArrayList<>();

        for (SupportTicket ticket : tickets) {

            Long loanAccountId = null;

            if (ticket.getLoanAccount() != null) {
                loanAccountId = ticket.getLoanAccount().getId();
            }

            SupportTicketResponse response =
                    new SupportTicketResponse(
                            ticket.getId(),
                            ticket.getCustomer().getId(),
                            loanAccountId,
                            ticket.getSubject(),
                            ticket.getDescription(),
                            ticket.getStatus(),
                            ticket.getCreatedAt(),
                            ticket.getUpdatedAt()
                    );

            responses.add(response);
        }

        log.info("All support tickets retrieved successfully");

        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public SupportTicketResponse getAdminTicket(Long ticketId) {

        log.info("Fetching admin support ticket with ID: {}", ticketId);

        SupportTicket ticket = supportTicketRepository
                .findById(ticketId)
                .orElseThrow(() -> {
                    log.warn("Support ticket not found with ID: {}", ticketId);
                    return new RuntimeException(
                            "Support ticket not found with id: " + ticketId
                    );
                });

        Long loanAccountId = null;

        if (ticket.getLoanAccount() != null) {
            loanAccountId = ticket.getLoanAccount().getId();
        }

        log.debug("Admin support ticket retrieved successfully: {}", ticketId);

        return new SupportTicketResponse(
                ticket.getId(),
                ticket.getCustomer().getId(),
                loanAccountId,
                ticket.getSubject(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }

    @Override
    @Transactional
    public SupportTicketResponse updateTicketStatus(
            Long ticketId,
            String status) {

        log.info("Updating status for support ticket ID: {}", ticketId);

        SupportTicket ticket = supportTicketRepository
                .findById(ticketId)
                .orElseThrow(() -> {
                    log.warn("Support ticket not found with ID: {}", ticketId);
                    return new RuntimeException(
                            "Support ticket not found with id: " + ticketId
                    );
                });

        if (!status.equals("OPEN")
                && !status.equals("IN_PROGRESS")
                && !status.equals("RESOLVED")
                && !status.equals("CLOSED")) {

            log.warn("Invalid status supplied for support ticket ID: {}", ticketId);
            throw new RuntimeException("Invalid ticket status");
        }

        ticket.setStatus(status);
        ticket.setUpdatedAt(LocalDateTime.now());

        SupportTicket updatedTicket =
                supportTicketRepository.save(ticket);

        Long loanAccountId = null;

        if (updatedTicket.getLoanAccount() != null) {
            loanAccountId =
                    updatedTicket.getLoanAccount().getId();
        }

        log.info("Support ticket status updated successfully: {}", ticketId);

        return new SupportTicketResponse(
                updatedTicket.getId(),
                updatedTicket.getCustomer().getId(),
                loanAccountId,
                updatedTicket.getSubject(),
                updatedTicket.getDescription(),
                updatedTicket.getStatus(),
                updatedTicket.getCreatedAt(),
                updatedTicket.getUpdatedAt()
        );
    }
}