package com.example.LoanMatrix.controller.EmiSchedule;


import com.example.LoanMatrix.entity.EmiSchedule;
import com.example.LoanMatrix.service.EmiScheduler.EmiSchedulerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/emi")
public class EmiScheduleController {

    private final EmiSchedulerService emiScheduleService;


    public EmiScheduleController(EmiSchedulerService emiScheduleService) {
        this.emiScheduleService = emiScheduleService;
    }
    @PostMapping("/schedule/{loanAccountId}")
    public ResponseEntity<List<EmiSchedule>> generateSchedule(
            @PathVariable Long loanAccountId) {

        return ResponseEntity.ok(emiScheduleService.generateSchedule(loanAccountId)
        );
    }

    @GetMapping("/schedule/{loanAccountId}")
    public ResponseEntity<List<EmiSchedule>> getSchedule(
            @PathVariable Long loanAccountId) {

        return ResponseEntity.ok(
                emiScheduleService.getSchedule(loanAccountId)
        );
    }

    @PostMapping("/recalculate/{loanAccountId}")
    public ResponseEntity<String> recalculateSchedule(
            @PathVariable Long loanAccountId) {

        emiScheduleService.recalculateSchedule(loanAccountId);

        return ResponseEntity.ok("EMI schedule recalculated successfully");
    }
    @PostMapping("/foreclosure/{loanAccountId}")
    public ResponseEntity<String> cancelFutureEmis(
            @PathVariable Long loanAccountId) {

        emiScheduleService.cancelFutureEmis(loanAccountId);

        return ResponseEntity.ok("Future EMIs cancelled successfully");
    }

}