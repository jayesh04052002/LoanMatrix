package com.example.LoanMatrix.controller.reports;

import com.example.LoanMatrix.dto.reports.CibilScoreRangeResponse;
import com.example.LoanMatrix.dto.reports.LoanPortfolioResponse;
import com.example.LoanMatrix.dto.reports.ProductWiseResponse;
import com.example.LoanMatrix.dto.reports.ScoreCardWiseResponse;
import com.example.LoanMatrix.response.ApiResponse;
import com.example.LoanMatrix.service.reports.ReportsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;
import com.example.LoanMatrix.dto.reports.DisbursementReportResponse;
import com.example.LoanMatrix.dto.reports.OverdueReportResponse;
import com.example.LoanMatrix.dto.reports.EmiCollectionResponse;
import com.example.LoanMatrix.dto.reports.PrepaymentReportResponse;

import java.util.List;

@RestController
@RequestMapping("/api/v1/officer/reports")
public class ReportsController {

    private final ReportsService reportsService;

    public ReportsController(ReportsService reportsService) {
        this.reportsService = reportsService;
    }

    @GetMapping("/cibil-score-range")
    public ResponseEntity<ApiResponse<List<CibilScoreRangeResponse>>>
    getCibilScoreRangeReport() {

        List<CibilScoreRangeResponse> report =
                reportsService.getCibilScoreRangeReport();

        ApiResponse<List<CibilScoreRangeResponse>> response =
                ApiResponse.<List<CibilScoreRangeResponse>>builder()
                        .success(true)
                        .message("CIBIL score range report fetched successfully")
                        .data(report)
                        .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/score-card-wise")
    public ResponseEntity<ApiResponse<List<ScoreCardWiseResponse>>>
    getScoreCardWiseReport() {

        List<ScoreCardWiseResponse> report =
                reportsService.getScoreCardWiseReport();

        ApiResponse<List<ScoreCardWiseResponse>> response =
                ApiResponse.<List<ScoreCardWiseResponse>>builder()
                        .success(true)
                        .message(
                                "Score card wise report fetched successfully")
                        .data(report)
                        .build();

        return ResponseEntity.ok(response);
    }

// Report 3: Product Wise Report
    @GetMapping("/product-wise")
    public ResponseEntity<ApiResponse<List<ProductWiseResponse>>>
    getProductWiseReport() {

        List<ProductWiseResponse> report =
                reportsService.getProductWiseReport();

        ApiResponse<List<ProductWiseResponse>> response =
                ApiResponse.<List<ProductWiseResponse>>builder()
                        .success(true)
                        .message(
                                "Product wise report fetched successfully")
                        .data(report)
                        .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/loan-portfolio")
    public ResponseEntity<ApiResponse<LoanPortfolioResponse>>
    getLoanPortfolioReport() {

        LoanPortfolioResponse report =
                reportsService.getLoanPortfolioReport();

        ApiResponse<LoanPortfolioResponse> response =
                ApiResponse.<LoanPortfolioResponse>builder()
                        .success(true)
                        .message("Loan portfolio report fetched successfully")
                        .data(report)
                        .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/emi-collection")
    public ResponseEntity<ApiResponse<EmiCollectionResponse>>
    getEmiCollectionReport() {

        EmiCollectionResponse report =
                reportsService.getEmiCollectionReport();

        ApiResponse<EmiCollectionResponse> response =
                ApiResponse.<EmiCollectionResponse>builder()
                        .success(true)
                        .message("EMI collection report fetched successfully")
                        .data(report)
                        .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/overdue")
    public ResponseEntity<ApiResponse<OverdueReportResponse>> getOverdueReport() {

        OverdueReportResponse result = reportsService.getOverdueReport();

        return ResponseEntity.ok(
                ApiResponse.<OverdueReportResponse>builder()
                        .success(true)
                        .message("Overdue report fetched successfully")
                        .data(result)
                        .build()
        );
    }

    @GetMapping("/disbursement")
    public ResponseEntity<ApiResponse<DisbursementReportResponse>>
    getDisbursementReport() {

        DisbursementReportResponse result =
                reportsService.getDisbursementReport();

        return ResponseEntity.ok(
                ApiResponse.<DisbursementReportResponse>builder()
                        .success(true)
                        .message("Disbursement report fetched successfully")
                        .data(result)
                        .build()
        );
    }

    @GetMapping("/prepayment")
    public ResponseEntity<ApiResponse<PrepaymentReportResponse>>
    getPrepaymentReport() {

        PrepaymentReportResponse result =
                reportsService.getPrepaymentReport();

        return ResponseEntity.ok(
                ApiResponse.<PrepaymentReportResponse>builder()
                        .success(true)
                        .message("Prepayment report fetched successfully")
                        .data(result)
                        .build()
        );
    }
}
