package com.example.icsm.repository;

import com.example.icsm.model.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Policy> {
    List<Policy> findByCustomerId(Long customerId);
    List<Policy> findByAgentId(Long agentId);
    long countByStatus(com.example.icsm.model.enums.PolicyStatus status);
    List<Policy> findByStatusAndNextPaymentDateLessThanEqual(com.example.icsm.model.enums.PolicyStatus status, java.time.LocalDate date);
    List<Policy> findByParentPolicyId(Long parentId);
    List<Policy> findByAgentIdAndParentPolicyIsNull(Long agentId);
    List<Policy> findByParentPolicyIsNullAndAgentIsNotNull();
}
