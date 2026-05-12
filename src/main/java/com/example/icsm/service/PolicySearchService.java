package com.example.icsm.service;

import com.example.icsm.dto.SearchCriteria;
import com.example.icsm.model.Policy;
import com.example.icsm.model.SearchQuery;
import com.example.icsm.model.User;
import com.example.icsm.repository.PolicyRepository;
import com.example.icsm.repository.SearchQueryRepository;
import com.example.icsm.specification.PolicySpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PolicySearchService {

    private final PolicyRepository policyRepository;
    private final SearchQueryRepository searchQueryRepository;

    public Page<Policy> searchPolicies(SearchCriteria criteria, Long customerId, Boolean isTemplate) {
        Specification<Policy> spec = Specification.where(PolicySpecification.withKeyword(criteria.getKeyword()))
                .and(PolicySpecification.withStatus(criteria.getStatus()))
                .and(PolicySpecification.withDateRange(criteria.getStartDate(), criteria.getEndDate()))
                .and(PolicySpecification.withCoverageRange(criteria.getMinCoverage(), criteria.getMaxCoverage()))
                .and(PolicySpecification.withAgent(criteria.getAgentId()))
                .and(PolicySpecification.withCustomer(customerId));

        if (Boolean.TRUE.equals(isTemplate)) {
            spec = spec.and(PolicySpecification.isTemplate());
        }

        Sort sort = Sort.by(Sort.Direction.fromString(criteria.getSortDir()), criteria.getSortBy());
        Pageable pageable = PageRequest.of(criteria.getPage(), criteria.getSize(), sort);

        return policyRepository.findAll(spec, pageable);
    }

    @Transactional
    public void saveSearchHistory(User user, SearchCriteria criteria, String searchUrl) {
        if (user == null) return;

        String queryText = criteria.getKeyword();
        StringBuilder filters = new StringBuilder();
        if (criteria.getStatus() != null) filters.append("Status: ").append(criteria.getStatus()).append("; ");
        if (criteria.getStartDate() != null) filters.append("From: ").append(criteria.getStartDate()).append("; ");
        if (criteria.getEndDate() != null) filters.append("To: ").append(criteria.getEndDate()).append("; ");
        if (criteria.getMinCoverage() != null) filters.append("Min: ").append(criteria.getMinCoverage()).append("; ");
        if (criteria.getMaxCoverage() != null) filters.append("Max: ").append(criteria.getMaxCoverage()).append("; ");

        SearchQuery searchQuery = SearchQuery.builder()
                .user(user)
                .queryText(queryText)
                .filtersDescription(filters.toString())
                .searchUrl(searchUrl)
                .build();

        searchQueryRepository.save(searchQuery);

        // Keep only last 10
        long count = searchQueryRepository.countByUser(user);
        if (count > 10) {
            List<SearchQuery> oldest = searchQueryRepository.findByUserOrderByCreatedAtDesc(user, PageRequest.of(10, (int)count - 10));
            searchQueryRepository.deleteAll(oldest);
        }
    }

    public List<SearchQuery> getSearchHistory(User user) {
        return searchQueryRepository.findByUserOrderByCreatedAtDesc(user, PageRequest.of(0, 10));
    }
}
