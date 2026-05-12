package com.example.icsm.dto;

import com.example.icsm.model.enums.PolicyStatus;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SearchCriteria {
    private String keyword;
    private PolicyStatus status;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
    
    private BigDecimal minCoverage;
    private BigDecimal maxCoverage;
    private Long agentId;
    
    private String sortBy = "id";
    private String sortDir = "desc";
    private int page = 0;
    private int size = 20;
}
