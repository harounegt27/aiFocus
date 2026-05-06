package com.example.aiFocus.service;

import com.example.aiFocus.dto.PeakHourStat;
import com.example.aiFocus.entity.BlockStatus;
import com.example.aiFocus.entity.FocusBlock;
import com.example.aiFocus.entity.ProductivityReport;
import com.example.aiFocus.entity.User;
import com.example.aiFocus.exception.ResourceNotFoundException;
import com.example.aiFocus.repository.FocusBlockRepository;
import com.example.aiFocus.repository.ProductivityReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ProductivityReportRepository reportRepository;

    @Mock
    private FocusBlockRepository focusBlockRepository;

    @Mock
    private AiService aiService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ReportService reportService;

    @Test
    void generateWeeklyReport_returnsExisting_whenAlreadyGenerated() {
        User user = new User();
        LocalDate weekStart = LocalDate.of(2024, 1, 1);
        ProductivityReport existingReport = new ProductivityReport();

        when(reportRepository.findByUserAndWeekStart(user, weekStart)).thenReturn(Optional.of(existingReport));

        ProductivityReport result = reportService.generateWeeklyReport(user, weekStart);

        assertThat(result).isEqualTo(existingReport);
        verify(aiService, never()).generateReportSummary(any(), any(), any(), any(), any());
    }

    @Test
    void generateWeeklyReport_calculatesStats_andCallsAi() {
        User user = new User();
        user.setName("Test User");
        LocalDate weekStart = LocalDate.of(2024, 1, 1);
        LocalDateTime from = weekStart.atStartOfDay();
        LocalDateTime to = weekStart.plusDays(7).atStartOfDay();

        FocusBlock block1 = new FocusBlock();
        block1.setStartTime(from.plusHours(9));
        block1.setEndTime(from.plusHours(10));

        FocusBlock block2 = new FocusBlock();
        block2.setStartTime(from.plusHours(10));
        block2.setEndTime(from.plusHours(11));

        FocusBlock skippedBlock = new FocusBlock();

        when(reportRepository.findByUserAndWeekStart(user, weekStart)).thenReturn(Optional.empty());
        when(focusBlockRepository.findByUserAndStatusAndStartTimeBetween(user, BlockStatus.COMPLETED, from, to))
                .thenReturn(List.of(block1, block2));
        when(focusBlockRepository.findByUserAndStatusAndStartTimeBetween(user, BlockStatus.SKIPPED, from, to))
                .thenReturn(List.of(skippedBlock));
        when(aiService.generateReportSummary(120, 2, 1, 9, "Test User")).thenReturn("Test summary");
        when(reportRepository.save(any(ProductivityReport.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductivityReport result = reportService.generateWeeklyReport(user, weekStart);

        assertThat(result.getTotalFocusMinutes()).isEqualTo(120);
        assertThat(result.getBlocksCompleted()).isEqualTo(2);
        assertThat(result.getBlocksSkipped()).isEqualTo(1);
        assertThat(result.getPeakHour()).isEqualTo(9);
        assertThat(result.getAiSummary()).isEqualTo("Test summary");
        verify(aiService).generateReportSummary(120, 2, 1, 9, "Test User");
        verify(reportRepository).save(any(ProductivityReport.class));
    }

    @Test
    void getWeeklyReport_throwsNotFound_whenMissing() {
        User user = new User();
        LocalDate weekStart = LocalDate.now().with(java.time.DayOfWeek.MONDAY);

        when(reportRepository.findByUserAndWeekStart(user, weekStart)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.getWeeklyReport(user))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No report found for week of " + weekStart);
    }

    @Test
    void getPeakHours_returnsCorrectDistribution() {
        User user = new User();
        LocalDateTime time1 = LocalDateTime.of(2024, 1, 1, 9, 0);
        LocalDateTime time2 = LocalDateTime.of(2024, 1, 2, 9, 0);
        LocalDateTime time3 = LocalDateTime.of(2024, 1, 3, 9, 0);
        LocalDateTime time4 = LocalDateTime.of(2024, 1, 4, 14, 0);

        FocusBlock block1 = new FocusBlock();
        block1.setStartTime(time1);
        FocusBlock block2 = new FocusBlock();
        block2.setStartTime(time2);
        FocusBlock block3 = new FocusBlock();
        block3.setStartTime(time3);
        FocusBlock block4 = new FocusBlock();
        block4.setStartTime(time4);

        when(focusBlockRepository.findByUserAndStatus(user, BlockStatus.COMPLETED))
                .thenReturn(List.of(block1, block2, block3, block4));

        List<PeakHourStat> result = reportService.getPeakHours(user);

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(new PeakHourStat(9, 3));
        assertThat(result.get(1)).isEqualTo(new PeakHourStat(14, 1));
    }
}
