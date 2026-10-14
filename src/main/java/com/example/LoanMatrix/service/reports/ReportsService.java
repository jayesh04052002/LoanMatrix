package com.example.LoanMatrix.service.reports;


import com.example.LoanMatrix.dto.reports.CibilScoreRangeResponse;
import com.example.LoanMatrix.dto.reports.LoanPortfolioResponse;
import com.example.LoanMatrix.dto.reports.ProductWiseResponse;
import com.example.LoanMatrix.dto.reports.ScoreCardWiseResponse;
import com.example.LoanMatrix.dto.reports.EmiCollectionResponse;
import com.example.LoanMatrix.dto.reports.OverdueReportResponse;
import com.example.LoanMatrix.dto.reports.DisbursementReportResponse;
import com.example.LoanMatrix.dto.reports.PrepaymentReportResponse;

import java.util.List;

public interface ReportsService {

    List<CibilScoreRangeResponse> getCibilScoreRangeReport();
    List<ScoreCardWiseResponse> getScoreCardWiseReport();
    List<ProductWiseResponse> getProductWiseReport();
    LoanPortfolioResponse getLoanPortfolioReport();
    EmiCollectionResponse getEmiCollectionReport();
    OverdueReportResponse getOverdueReport();
    DisbursementReportResponse getDisbursementReport();
    PrepaymentReportResponse getPrepaymentReport();

}