package com.example.icsm.config;

import com.example.icsm.model.*;
import com.example.icsm.model.enums.*;
import com.example.icsm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PolicyCategoryRepository categoryRepository;
    private final PolicyTypeRepository typeRepository;
    private final PolicyRepository policyRepository;
    private final NotificationRepository notificationRepository;
    private final ClaimRepository claimRepository;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) return;

        // 1. Create User
        User user = User.builder()
                .fullName("John Doe")
                .email("john@example.com")
                .passwordHash("hashed_password")
                .role(UserRole.Customer)
                .status(UserStatus.active)
                .build();
        userRepository.save(user);

        // 2. Create Categories
        PolicyCategory healthCat = PolicyCategory.builder()
                .name("Health Insurance")
                .description("Comprehensive medical coverage")
                .build();
        categoryRepository.save(healthCat);

        // 3. Create Types
        PolicyType premiumHealth = PolicyType.builder()
                .category(healthCat)
                .name("Premium Health Plan")
                .description("All-inclusive coverage including dental and vision.")
                .build();
        typeRepository.save(premiumHealth);

        // 4. Create Policies
        Policy policy = Policy.builder()
                .customer(user)
                .policyType(premiumHealth)
                .coverageAmount(new BigDecimal("500000"))
                .premiumAmount(new BigDecimal("120.00"))
                .paymentFrequency(PaymentFrequency.monthly)
                .status(PolicyStatus.Active)
                .startDate(LocalDate.now().minusMonths(1))
                .endDate(LocalDate.now().plusYears(1))
                .build();
        policyRepository.save(policy);

        // 5. Create Notifications
        notificationRepository.saveAll(List.of(
                Notification.builder()
                        .user(user)
                        .title("Welcome to ShieldGuard")
                        .message("Your account has been successfully created.")
                        .type(NotificationType.system)
                        .isRead(false)
                        .build(),
                Notification.builder()
                        .user(user)
                        .title("Policy Activated")
                        .message("Your Premium Health Plan is now active.")
                        .type(NotificationType.claim_update)
                        .isRead(false)
                        .build()
        ));

        // 6. Create Claim
        Claim claim = Claim.builder()
                .customer(user)
                .policy(policy)
                .description("Hospitalization due to flu.")
                .claim_amount(new BigDecimal("1500.00"))
                .status(ClaimStatus.Under_Review)
                .build();
        claimRepository.save(claim);

        System.out.println("Dummy data initialized.");
    }
}
