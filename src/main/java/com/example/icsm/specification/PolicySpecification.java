package com.example.icsm.specification;

import com.example.icsm.model.Policy;
import com.example.icsm.model.enums.PolicyStatus;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Join;
import java.math.BigDecimal;
import java.time.LocalDate;

public class PolicySpecification {

    public static Specification<Policy> withKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;
            String pattern = "%" + keyword.toLowerCase() + "%";
            
            // Use LEFT JOINS to avoid excluding policies with null relationships
            jakarta.persistence.criteria.Join<Object, Object> customerJoin = root.join("customer", jakarta.persistence.criteria.JoinType.LEFT);
            jakarta.persistence.criteria.Join<Object, Object> policyTypeJoin = root.join("policyType", jakarta.persistence.criteria.JoinType.LEFT);

            // Try to parse keyword as Long for ID matching
            try {
                Long id = Long.parseLong(keyword);
                return cb.or(
                    cb.equal(root.get("id"), id),
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(customerJoin.get("fullName")), pattern),
                    cb.like(cb.lower(policyTypeJoin.get("name")), pattern)
                );
            } catch (NumberFormatException e) {
                return cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(customerJoin.get("fullName")), pattern),
                    cb.like(cb.lower(policyTypeJoin.get("name")), pattern)
                );
            }
        };
    }

    public static Specification<Policy> withStatus(PolicyStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Policy> withDateRange(LocalDate start, LocalDate end) {
        return (root, query, cb) -> {
            if (start == null && end == null) return null;
            if (start != null && end != null) return cb.between(root.get("startDate"), start, end);
            if (start != null) return cb.greaterThanOrEqualTo(root.get("startDate"), start);
            return cb.lessThanOrEqualTo(root.get("startDate"), end);
        };
    }

    public static Specification<Policy> withCoverageRange(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;
            if (min != null && max != null) return cb.between(root.get("coverageAmount"), min, max);
            if (min != null) return cb.greaterThanOrEqualTo(root.get("coverageAmount"), min);
            return cb.lessThanOrEqualTo(root.get("coverageAmount"), max);
        };
    }

    public static Specification<Policy> withAgent(Long agentId) {
        return (root, query, cb) -> agentId == null ? null : cb.equal(root.get("agent").get("id"), agentId);
    }

    public static Specification<Policy> withCustomer(Long customerId) {
        return (root, query, cb) -> customerId == null ? null : cb.equal(root.get("customer").get("id"), customerId);
    }
    
    public static Specification<Policy> isTemplate() {
        return (root, query, cb) -> cb.isNull(root.get("parentPolicy"));
    }
}
