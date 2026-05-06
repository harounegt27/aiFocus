package com.example.aiFocus.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.example.aiFocus.entity.User;

@Entity
@Table(name = "productivity_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductivityReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDate weekStart;

    private Integer totalFocusMinutes;

    private Integer blocksCompleted;

    private Integer blocksSkipped;

    private Integer peakHour;

    @Column(columnDefinition = "text")
    private String aiSummary;

    private LocalDateTime generatedAt;

    @PrePersist
    public void prePersist() {
        this.generatedAt = LocalDateTime.now();
    }
}
