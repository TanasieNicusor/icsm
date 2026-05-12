package com.example.icsm.repository;

import com.example.icsm.model.SearchQuery;
import com.example.icsm.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SearchQueryRepository extends JpaRepository<SearchQuery, Long> {
    List<SearchQuery> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    void deleteByUserOrderByCreatedAtAsc(User user);
    
    long countByUser(User user);
}
