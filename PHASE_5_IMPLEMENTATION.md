# Phase 5: Notifications Implementation Guide

## Overview

Phase 5 implements a complete email notification system for the aiFocus application. Users receive:
1. **Confirmation Email** - Sent immediately when a focus block is created
2. **Reminder Email** - Sent 15 minutes before the block starts (via scheduled task)

---

## Implementation Summary

### Files Created

#### Core Implementation (4 files)
- `src/main/java/com/example/aiFocus/config/MailConfig.java` - SMTP configuration
- `src/main/java/com/example/aiFocus/service/EmailService.java` - Email service layer
- `src/main/java/com/example/aiFocus/scheduler/ReminderScheduler.java` - Scheduled reminder task
- `src/main/java/com/example/aiFocus/listener/FocusBlockEventListener.java` - Event listener

#### Test Files (2 files)
- `src/test/java/com/example/aiFocus/scheduler/ReminderSchedulerTest.java` - Scheduler tests
- `src/test/java/com/example/aiFocus/listener/FocusBlockEventListenerTest.java` - Listener tests

### Files Modified

| File | Change |
|------|--------|
| `repository/FocusBlockRepository.java` | Added: `findByStatus(BlockStatus status)` |
| `src/main/resources/application.properties` | Added: Mail SMTP configuration |
| `src/test/resources/application.properties` | Added: Test mail configuration |
| `AiFocusApplication.java` | Added: `@EnableScheduling` annotation |

---

## Detailed Implementation

### 1. MailConfig.java

**Purpose**: Configure the JavaMailSender bean for SMTP connections

**Key Features**:
- Reads configuration from `application.properties`
- Supports Gmail SMTP with TLS/STARTTLS encryption
- Constructor-based property injection
- Complete Javadoc documentation

**Configuration Properties**:
```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=YOUR_EMAIL@gmail.com
spring.mail.password=YOUR_APP_PASSWORD
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

### 2. EmailService.java

**Purpose**: Handle all email sending logic

**Methods**:
- `sendFocusReminder(User user, FocusBlock block)` - Sends reminder email
  - Subject: "Focus Block Starting Soon — [date]"
  - Body: Includes user name, start time, end time, motivational message
  
- `sendFocusBlockConfirmation(User user, FocusBlock block)` - Sends confirmation email
  - Subject: "Focus Block Confirmed"
  - Body: Includes block details and reminder timing information

**Exception Handling**:
- Catches MailException gracefully
- Logs errors without re-throwing
- Prevents scheduler from crashing

**Dependency Injection**:
- Constructor injection only (no field @Autowired)
- Injects JavaMailSender and @Value fromEmail

### 3. ReminderScheduler.java

**Purpose**: Periodically check and send reminder emails

**Scheduling**:
- Executes every 5 minutes (300000 ms)
- Requires `@EnableScheduling` on main application class

**Logic**:
1. Retrieves all SCHEDULED focus blocks from repository
2. Checks each block's start time against current time
3. Sends reminder if start time is between now+10min and now+20min
4. The 10-20 minute window prevents duplicate reminders

**Exception Handling**:
- Catches exceptions silently
- Logs errors without interrupting scheduler
- Ensures scheduler continues running

### 4. FocusBlockEventListener.java

**Purpose**: Listen for focus block events and send confirmation emails

**Event Handling**:
- Listens to `FocusBlockScheduledEvent`
- Triggered when `FocusService.confirmBlock()` publishes the event
- Sends confirmation email immediately

**Implementation**:
- Uses `@EventListener` annotation
- Constructor injection of EmailService
- Exception handling to prevent event propagation failures

### 5. FocusBlockRepository.java (Updated)

**New Method**:
```java
List<FocusBlock> findByStatus(BlockStatus status);
```

Used by ReminderScheduler to retrieve all SCHEDULED blocks.

### 6. AiFocusApplication.java (Updated)

**Addition**:
```java
@SpringBootApplication
@EnableScheduling
public class AiFocusApplication {
    // ...
}
```

The `@EnableScheduling` annotation enables the `@Scheduled` tasks in ReminderScheduler.

---

## Email Flow

### Confirmation Email Flow
```
1. User creates/confirms a focus block
   ↓
2. FocusService.confirmBlock() saves block
   ↓
3. FocusBlockScheduledEvent is published
   ↓
4. FocusBlockEventListener receives event
   ↓
5. EmailService.sendFocusBlockConfirmation() is called
   ↓
6. Confirmation email sent immediately
```

### Reminder Email Flow
```
Every 5 minutes:
1. ReminderScheduler.checkAndSendReminders() executes
   ↓
2. Query all SCHEDULED blocks from repository
   ↓
3. For each block:
   - Check if startTime is between (now+10min) and (now+20min)
   - If yes: EmailService.sendFocusReminder()
   ↓
4. Email sent to user
```

---

## Gmail Configuration

### Prerequisites
1. Gmail account with 2-Factor Authentication enabled
2. App Password generated for the email service

### Setup Steps

#### 1. Enable 2FA on Gmail
- Go to https://myaccount.google.com
- Enable 2-Factor Authentication

#### 2. Generate App Password
- Go to https://myaccount.google.com/apppasswords
- Select "Mail" application
- Select your device/computer
- Generate and copy the 16-character password

#### 3. Configure application.properties
```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=xxxx xxxx xxxx xxxx
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

⚠️ **Important**: Use the App Password, NOT your Gmail login password

#### 4. Test Configuration
Create a focus block and verify:
- Confirmation email received immediately
- Reminder email received ~15 minutes before scheduled time

---

## Unit Tests

### ReminderSchedulerTest.java

**Test Cases**:

1. `sendsReminder_whenBlockIsWithin10To20Minutes`
   - Creates a block starting in 15 minutes
   - Verifies email is sent

2. `doesNotSendReminder_whenBlockIsTooFarAway`
   - Creates a block starting in 60 minutes
   - Verifies email is NOT sent

3. `doesNotSendReminder_whenBlockAlreadyStarted`
   - Creates a block that started 5 minutes ago
   - Verifies email is NOT sent

### FocusBlockEventListenerTest.java

**Test Cases**:

1. `onEvent_sendsConfirmationEmail`
   - Publishes a FocusBlockScheduledEvent
   - Verifies confirmation email is sent

---

## Build & Test Commands

```bash
# Compile without tests
mvnw clean compile

# Run only Phase 5 tests
mvnw test -Dtest=ReminderSchedulerTest
mvnw test -Dtest=FocusBlockEventListenerTest

# Run all tests
mvnw test

# Build for production (skip tests)
mvnw clean package -DskipTests

# Run application
mvnw spring-boot:run
```

---

## Configuration Files Updated

### application.properties

Added mail configuration:
```properties
# Mail Configuration
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=YOUR_GMAIL@gmail.com
spring.mail.password=YOUR_APP_PASSWORD
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

### test/resources/application.properties

Added test mail configuration:
```properties
# Mail Configuration (test defaults)
spring.mail.host=localhost
spring.mail.port=587
spring.mail.username=test@example.com
spring.mail.password=test
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

---

## Code Quality Standards

✅ **Implemented**:
- Constructor injection only (no @Autowired on fields)
- Complete Javadoc on all classes and public methods
- Proper exception handling and logging
- No modifications to pom.xml
- No modifications to Flyway migration files
- No use of deprecated Spring APIs
- Tests provided for new components
- No breaking changes to existing code

---

## Error Handling

### EmailService Exception Handling
- Catches `MailException` without rethrowing
- Logs all mail errors
- Failed emails do not disrupt application flow

### ReminderScheduler Exception Handling
- Catches all exceptions in scheduled method
- Logs errors without interrupting scheduler
- Scheduler continues executing on the next cycle

### FocusBlockEventListener Exception Handling
- Catches exceptions to prevent event propagation failure
- Logs errors for debugging

---

## Logging

Application logs email sending:

```
# Confirmation email
INFO ... FocusBlockEventListener : Focus block confirmation email sent for block: 1

# Reminder email  
INFO ... ReminderScheduler : Reminder sent to john@example.com for block starting at ...

# Scheduler execution
DEBUG ... ReminderScheduler : Starting reminder check...
DEBUG ... ReminderScheduler : Reminder check completed successfully
```

---

## Integration with Existing Phases

**Phase 5 depends on**:
- Phase 1: User Registration (User entity)
- Phase 2: JWT Authentication (Bearer token auth)
- Phase 4: Focus Management (FocusBlock entity and service)

**Phase 5 does not modify**:
- User authentication flow
- Focus block creation/update logic
- Database schema (Flyway migrations)
- Security configuration

---

## Production Deployment

### Pre-deployment Checklist
- [ ] Gmail account with 2FA enabled
- [ ] App Password generated
- [ ] `spring.mail.username` configured
- [ ] `spring.mail.password` configured with App Password
- [ ] Port 587 accessible for SMTP
- [ ] Test email sent successfully
- [ ] Application logs configured

### Environment Configuration
Store sensitive values in environment variables:
```bash
SPRING_MAIL_USERNAME=${GMAIL_ACCOUNT}
SPRING_MAIL_PASSWORD=${GMAIL_APP_PASSWORD}
```

### Monitoring
Monitor logs for:
- Failed email sends
- Scheduler execution timing
- Exception occurrences

---

## Alternative Email Providers

The application can use any SMTP provider by updating configuration:
- SendGrid
- AWS SES
- Mailgun
- Brevo (formerly Sendinblue)

Update `spring.mail.host` and credentials accordingly.

---

## Implementation Specifications Met

✅ All requirements from Phase 5 specification implemented:

1. **MailConfig.java** - JavaMailSender bean with SMTP configuration
2. **EmailService.java** - Email sending service with proper exception handling
3. **ReminderScheduler.java** - 5-minute scheduled task for reminders
4. **FocusBlockEventListener.java** - Event listener for confirmations
5. **Repository update** - findByStatus method added
6. **Application properties** - Mail configuration added
7. **Test properties** - Test mail configuration added
8. **AiFocusApplication.java** - @EnableScheduling annotation
9. **Unit tests** - All test cases provided
10. **Documentation** - Complete implementation guide

---

## Quick Start

### 1. Generate Gmail App Password (2 minutes)
- Visit https://myaccount.google.com/apppasswords
- Copy the 16-character password

### 2. Configure application.properties (1 minute)
```properties
spring.mail.username=your-email@gmail.com
spring.mail.password=<app-password>
```

### 3. Compile and Run (2 minutes)
```bash
mvnw clean compile
mvnw spring-boot:run
```

### 4. Test (1 minute)
- Create a focus block
- Check email for confirmation
- Wait 5 minutes for reminder

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| "Could not resolve placeholder 'spring.mail.host'" | Verify mail properties are in application.properties |
| "Invalid login credentials" | Use App Password, not Gmail password |
| No email received | Check Spam folder, verify email address in logs |
| "Connection refused" | Ensure port 587 is accessible |
| Scheduler not running | Verify @EnableScheduling is in AiFocusApplication |

---

## Support & Documentation

All code includes complete Javadoc:
- Hover over class/method names in IDE for documentation
- Check method implementations for detailed logic

---

## Status

**✅ COMPLETE & PRODUCTION READY**

- All files created and tested
- Compilation successful
- Tests passing
- Documentation complete
- Ready for deployment

---

*Implementation Date: 2026-05-06*  
*Status: Complete*  
*Spring Boot Version: 3.4.5*  
*Java Version: 17*

