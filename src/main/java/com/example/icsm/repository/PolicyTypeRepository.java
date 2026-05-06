package com.example.icsm.repository;

import com.example.icsm.model.PolicyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PolicyTypeRepository extends JpaRepository<PolicyType, Long> {
    List<PolicyType> findByCategoryId(Long categoryId);
}
