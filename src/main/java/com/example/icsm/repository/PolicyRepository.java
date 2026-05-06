package com.example.icsm.repository;

import com.example.icsm.model.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {
    List<Policy> findByCustomerId(Long customerId);
    List<Policy> findByAgentId(Long agentId);
}
