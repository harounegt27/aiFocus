# Phase 6 Implementation: Weekly Productivity Reports

## Overview
Implemented weekly productivity reports feature for the aiFocus Spring Boot application. This phase adds automated generation and emailing of weekly reports summarizing user focus block activity, including AI-generated insights.

## New Files Created

### Entities
- `entity/ProductivityReport.java` - JPA entity for storing weekly productivity reports with metrics and AI summaries

### Repositories
- `repository/ProductivityReportRepository.java` - Repository for productivity reports with custom query methods

### DTOs
- `dto/ReportResponse.java` - Response DTO for report data
- `dto/PeakHourStat.java` - DTO for peak hour statistics

### Services
- `service/ReportService.java` - Core service for generating and retrieving reports
- Updated `service/AiService.java` - Added `generateReportSummary()` method for AI-powered report summaries
- Updated `service/EmailService.java` - Added `sendWeeklyReport()` method for email notifications

### Controllers
- `controller/ReportController.java` - REST endpoints for report access:
  - `GET /api/reports/weekly` - Current week report
  - `GET /api/reports/peak-hours` - Peak productivity hours
  - `GET /api/reports/history` - Last 4 weeks history

### Schedulers
- `scheduler/WeeklyReportScheduler.java` - Scheduled task running every Monday at 8 AM to generate and send reports

### Tests
- `test/service/ReportServiceTest.java` - Unit tests for report generation logic

## Key Features
- Automatic weekly report generation with focus metrics calculation
- AI-generated summaries using Groq API
- Email notifications with detailed productivity insights
- REST API for report retrieval
- Peak hour analysis for productivity patterns
- Report history tracking

## Database Changes
- New `productivity_reports` table (via V4 migration)
- Added query method to `FocusBlockRepository` for time-range filtering

## Security
- All report endpoints require authentication
- Reports are user-specific and access-controlled

## Scheduling
- Reports generated and emailed every Monday morning
- Graceful error handling to prevent failures from stopping batch processing
