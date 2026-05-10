package com.example.icsm.repository;

import com.example.icsm.model.Claim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {
    List<Claim> findByCustomerId(Long customerId);
    List<Claim> findByPolicyId(Long policyId);
    long countByStatus(com.example.icsm.model.enums.ClaimStatus status);
}
