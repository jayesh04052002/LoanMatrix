package com.example.LoanMatrix.controller.deal;

import com.example.LoanMatrix.dto.deal.ApplyLoanRequestDto;
import com.example.LoanMatrix.dto.deal.ApplyLoanResponseDto;
import com.example.LoanMatrix.dto.deal.LoanTypeOptionDto;
import com.example.LoanMatrix.dto.deal.ReviewDealRequestDto;
import com.example.LoanMatrix.response.ApiResponse;
import com.example.LoanMatrix.service.deal.ApplyLoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ApplyLoanController {

    private final ApplyLoanService applyLoanService;

    /**
     * 1. Get available loan types and ROI options
     */
    @GetMapping("/types")
    public ResponseEntity<ApiResponse<List<LoanTypeOptionDto>>> getAvailableLoanTypes() {
        log.info("REST request to fetch available loan schemes and ROI options");
        List<LoanTypeOptionDto> types = applyLoanService.getAvailableLoanTypes();

        return ResponseEntity.ok(
                ApiResponse.<List<LoanTypeOptionDto>>builder()
                        .success(true)
                        .message("Available loan types fetched successfully.")
                        .data(types)
                        .build()
        );
    }

    /**
     * 2. Customer: Apply for a new loan deal
     */
    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<ApplyLoanResponseDto>> applyLoan(@Valid @RequestBody ApplyLoanRequestDto requestDto) {
        log.info("REST request to apply for a loan for Customer ID: {}, Type: {}, Amount: {}",
                requestDto.getCustomerId(), requestDto.getLoanType(), requestDto.getLoanAmount());

        ApplyLoanResponseDto responseDto = applyLoanService.applyForLoan(requestDto);

        return new ResponseEntity<>(
                ApiResponse.<ApplyLoanResponseDto>builder()
                        .success(true)
                        .message("Loan application submitted successfully. Status: PENDING")
                        .data(responseDto)
                        .build(),
                HttpStatus.CREATED
        );
    }

    /**
     * 3. Customer: View all loan applications for their customerId
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<List<ApplyLoanResponseDto>>> getCustomerApplications(@PathVariable Long customerId) {
        log.info("REST request to fetch loan applications for Customer ID: {}", customerId);

        List<ApplyLoanResponseDto> list = applyLoanService.getCustomerApplications(customerId);

        return ResponseEntity.ok(
                ApiResponse.<List<ApplyLoanResponseDto>>builder()
                        .success(true)
                        .message("Fetched customer loan applications successfully.")
                        .data(list)
                        .build()
        );
    }

    /**
     * 4. Loan Officer: Fetch all pending deals (unreviewed deals)
     */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<ApplyLoanResponseDto>>> getPendingDeals() {
        log.info("REST request to fetch all pending deals awaiting officer review");

        List<ApplyLoanResponseDto> pendingDeals = applyLoanService.getPendingDeals();

        return ResponseEntity.ok(
                ApiResponse.<List<ApplyLoanResponseDto>>builder()
                        .success(true)
                        .message("Fetched pending deals successfully.")
                        .data(pendingDeals)
                        .build()
        );
    }

    /**
     * 5. Loan Officer: Review a deal (APPROVED / REJECTED)
     */
    @PutMapping("/{dealId}/review")
    public ResponseEntity<ApiResponse<ApplyLoanResponseDto>> reviewDeal(
            @PathVariable Long dealId,
            @Valid @RequestBody ReviewDealRequestDto requestDto) {

        log.info("REST request to review Deal ID: {} by Officer ID: {} with Decision: {}",
                dealId, requestDto.getOfficerId(), requestDto.getStatus());

        ApplyLoanResponseDto reviewedDeal = applyLoanService.reviewDeal(
                dealId,
                requestDto.getOfficerId(),
                requestDto.getStatus()
        );

        return ResponseEntity.ok(
                ApiResponse.<ApplyLoanResponseDto>builder()
                        .success(true)
                        .message("Deal successfully updated to " + requestDto.getStatus().toUpperCase())
                        .data(reviewedDeal)
                        .build()
        );
    }

    /**
     * 6. Fetch all deals that are APPROVED (Used by Officer to Generate Sanction Letters)
     */
    @GetMapping("/approved")
    public ResponseEntity<ApiResponse<List<ApplyLoanResponseDto>>> getApprovedDeals() {
        log.info("REST request to fetch all approved deals for sanction generation");
        List<ApplyLoanResponseDto> approvedDeals = applyLoanService.getApprovedDeals();

        return ResponseEntity.ok(
                ApiResponse.<List<ApplyLoanResponseDto>>builder()
                        .success(true)
                        .message("Fetched approved deals successfully.")
                        .data(approvedDeals)
                        .build()
        );
    }

    /**
     * 7. Fetch all deals regardless of status
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<ApplyLoanResponseDto>>> getAllDeals() {
        log.info("REST request to fetch all deals");
        List<ApplyLoanResponseDto> allDeals = applyLoanService.getAllDeals();

        return ResponseEntity.ok(
                ApiResponse.<List<ApplyLoanResponseDto>>builder()
                        .success(true)
                        .message("Fetched all deals successfully.")
                        .data(allDeals)
                        .build()
        );
    }

}