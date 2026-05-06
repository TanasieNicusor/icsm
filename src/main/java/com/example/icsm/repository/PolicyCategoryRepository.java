package com.example.icsm.repository;

import com.example.icsm.model.PolicyCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PolicyCategoryRepository extends JpaRepository<PolicyCategory, Long> {
}
