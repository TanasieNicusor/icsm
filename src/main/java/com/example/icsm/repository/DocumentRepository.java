package com.example.icsm.repository;

import com.example.icsm.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByUserId(Long userId);
    List<Document> findByPolicyId(Long policyId);
    List<Document> findByClaimId(Long claimId);
}
