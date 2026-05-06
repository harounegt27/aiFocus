package com.example.aiFocus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import com.example.aiFocus.entity.ProductivityReport;
import com.example.aiFocus.entity.User;

public interface ProductivityReportRepository extends JpaRepository<ProductivityReport, Long> {

    Optional<ProductivityReport> findByUserAndWeekStart(User user, LocalDate weekStart);

    List<ProductivityReport> findByUserOrderByWeekStartDesc(User user);

    List<ProductivityReport> findTop4ByUserOrderByWeekStartDesc(User user);
}
