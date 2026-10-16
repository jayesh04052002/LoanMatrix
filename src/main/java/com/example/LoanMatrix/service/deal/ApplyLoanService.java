package com.example.LoanMatrix.service.deal;

import com.example.LoanMatrix.dto.deal.ApplyLoanRequestDto;
import com.example.LoanMatrix.dto.deal.ApplyLoanResponseDto;
import com.example.LoanMatrix.dto.deal.LoanTypeOptionDto;
import com.example.LoanMatrix.entity.LoanDeal;

import java.util.List;

public interface ApplyLoanService {
    ApplyLoanResponseDto applyForLoan(ApplyLoanRequestDto requestDto);
    List<ApplyLoanResponseDto> getCustomerApplications(Long customerId);
    List<ApplyLoanResponseDto> getPendingDeals();
    ApplyLoanResponseDto reviewDeal(Long dealId, Long officerId, String status);
    ApplyLoanResponseDto deriveStatusAndMap(LoanDeal deal);
    List<LoanTypeOptionDto> getAvailableLoanTypes();


    List<ApplyLoanResponseDto> getApprovedDeals();
    List<ApplyLoanResponseDto> getAllDeals();
}