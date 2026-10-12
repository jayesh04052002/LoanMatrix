package com.example.LoanMatrix.service.EmiScheduler;

import com.example.LoanMatrix.entity.EmiSchedule;

import java.util.List;

public interface EmiSchedulerService {

    List<EmiSchedule> generateSchedule(Long loanAccountId);

    List<EmiSchedule> getSchedule(Long loanAccountId);

    void recalculateSchedule(Long loanAccountId);

    void cancelFutureEmis(Long loanAccountId);
}