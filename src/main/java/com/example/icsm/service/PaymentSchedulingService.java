package com.example.icsm.service;

import com.example.icsm.model.Invoice;
import com.example.icsm.model.Policy;
import com.example.icsm.model.enums.InvoiceStatus;
import com.example.icsm.model.enums.NotificationType;
import com.example.icsm.model.enums.PolicyStatus;
import com.example.icsm.repository.InvoiceRepository;
import com.example.icsm.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentSchedulingService {

    private final PolicyRepository policyRepository;
    private final InvoiceRepository invoiceRepository;
    private final NotificationService notificationService;
    private final SystemConfigService systemConfigService;

    // Run every day at midnight (0 0 0 * * ?)
    // For testing/demonstration, we can run it every minute: @Scheduled(cron = "0 * * * * ?")
    @Scheduled(cron = "0 0 0 * * ?")
    public void processDailyPayments() {
        log.info("Starting daily payment processing jobs...");
        generateInvoices();
        enforceLateFees();
        autoSuspendPolicies();
        log.info("Daily payment processing completed.");
    }

    /**
     * REQ-34, REQ-35: Automatically generate a payment invoice for each active policy 
     * based on its payment frequency on the scheduled due date.
     */
    private void generateInvoices() {
        LocalDate today = LocalDate.now();
        List<Policy> duePolicies = policyRepository.findByStatusAndNextPaymentDateLessThanEqual(PolicyStatus.Active, today);

        for (Policy policy : duePolicies) {
            // Generate Invoice
            Invoice invoice = Invoice.builder()
                    .policy(policy)
                    .customer(policy.getCustomer())
                    .invoiceNumber("INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .amountDue(policy.getPremiumAmount())
                    .dueDate(today)
                    .gracePeriodDays(systemConfigService.getGracePeriodDays())
                    .status(InvoiceStatus.Pending)
                    .build();
            
            invoiceRepository.save(invoice);

            // Notify Customer
            notificationService.createNotification(policy.getCustomer(), null,
                    "New Invoice Generated",
                    "A new invoice (" + invoice.getInvoiceNumber() + ") of $" + invoice.getAmountDue() + " is due for your policy: " + policy.getName() + ".",
                    NotificationType.payment_reminder);

            // Update Policy Next Payment Date
            LocalDate nextDate = calculateNextPaymentDate(policy.getNextPaymentDate(), policy.getPaymentFrequency());
            policy.setNextPaymentDate(nextDate);
            policyRepository.save(policy);
            
            log.info("Generated invoice {} for policy {}", invoice.getInvoiceNumber(), policy.getId());
        }
    }

    /**
     * REQ-36, REQ-37: Enforce grace period and calculate/apply late fees.
     */
    private void enforceLateFees() {
        LocalDate today = LocalDate.now();
        int gracePeriod = systemConfigService.getGracePeriodDays();
        LocalDate cutoffDate = today.minusDays(gracePeriod);

        // Find pending invoices where due date is older than the cutoff
        List<Invoice> overdueInvoices = invoiceRepository.findByStatusAndDueDateLessThanEqual(InvoiceStatus.Pending, cutoffDate);

        for (Invoice invoice : overdueInvoices) {
            if (!invoice.isLateFeeApplied()) {
                invoice.setLateFeeAmount(systemConfigService.getLateFeeAmount());
                invoice.setAmountDue(invoice.getAmountDue().add(invoice.getLateFeeAmount()));
                invoice.setLateFeeApplied(true);
                invoice.setStatus(InvoiceStatus.Overdue); // Explicitly mark as overdue
                invoiceRepository.save(invoice);

                // Notify Customer
                notificationService.createNotification(invoice.getCustomer(), null,
                        "Invoice Overdue - Late Fee Applied",
                        "Your invoice (" + invoice.getInvoiceNumber() + ") is past the grace period. A late fee of $" + invoice.getLateFeeAmount() + " has been added.",
                        NotificationType.payment_reminder);
                        
                log.info("Applied late fee to invoice {}", invoice.getInvoiceNumber());
            }
        }
    }

    /**
     * REQ-38: Automatically change a policy's status to "Suspended" if unpaid > 60 days past due date.
     */
    private void autoSuspendPolicies() {
        LocalDate today = LocalDate.now();
        LocalDate suspensionCutoff = today.minusDays(60);

        // Find overdue invoices older than 60 days past due
        List<Invoice> criticallyOverdue = invoiceRepository.findByStatusAndDueDateLessThanEqual(InvoiceStatus.Overdue, suspensionCutoff);

        for (Invoice invoice : criticallyOverdue) {
            Policy policy = invoice.getPolicy();
            if (policy.getStatus() == PolicyStatus.Active) {
                policy.setStatus(PolicyStatus.Suspended);
                policyRepository.save(policy);

                // Notify Customer
                notificationService.createNotification(policy.getCustomer(), null,
                        "Policy Suspended",
                        "Your policy " + policy.getName() + " has been suspended due to unpaid invoices exceeding 60 days past the due date.",
                        NotificationType.system);
                        
                // Notify Agent
                if (policy.getAgent() != null) {
                    notificationService.createNotification(policy.getAgent(), null,
                            "Client Policy Suspended",
                            "Policy " + policy.getName() + " for client " + policy.getCustomer().getFullName() + " has been suspended for non-payment.",
                            NotificationType.system);
                }
                
                log.info("Suspended policy {} due to critically overdue invoice {}", policy.getId(), invoice.getInvoiceNumber());
            }
        }
    }

    private LocalDate calculateNextPaymentDate(LocalDate currentDueDate, com.example.icsm.model.enums.PaymentFrequency frequency) {
        if (currentDueDate == null) currentDueDate = LocalDate.now();
        
        switch (frequency) {
            case monthly:
                return currentDueDate.plusMonths(1);
            case quarterly:
                return currentDueDate.plusMonths(3);
            case yearly:
                return currentDueDate.plusYears(1);
            default:
                return currentDueDate.plusMonths(1); // Default to monthly
        }
    }
}
