package com.example.icsm.model;

import com.example.icsm.model.enums.ReportType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = true)
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportType type;

    @Column(columnDefinition = "TEXT")
    private String parameters;

    @Column(name = "file_url")
    private String fileUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;
}
