package com.banking.system.services;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class LoanSchedulerService {

    private final LoanService loanService;

    public LoanSchedulerService(LoanService loanService) {
        this.loanService = loanService;
    }

    // Runs every day at 3:00 AM
    @Scheduled(cron = "0 0 3 * * *")
    public void checkOverdueRepayments() {
        loanService.markOverdueRepayments();
    }
}