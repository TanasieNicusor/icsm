package com.example.icsm.repository;

import com.example.icsm.model.ChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChangeLogRepository extends JpaRepository<ChangeLog, Long> {

    @Query("SELECT c FROM ChangeLog c WHERE " +
           "(:entityName IS NULL OR c.entityName = :entityName) AND " +
           "(:changedBy IS NULL OR c.changedBy LIKE %:changedBy%) AND " +
           "(:startDate IS NULL OR c.changedAt >= :startDate) AND " +
           "(:endDate IS NULL OR c.changedAt <= :endDate) " +
           "ORDER BY c.changedAt DESC")
    List<ChangeLog> findByFilters(@Param("entityName") String entityName,
                                  @Param("changedBy") String changedBy,
                                  @Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);
}
