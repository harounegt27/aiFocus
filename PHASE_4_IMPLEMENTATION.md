# Phase 4 Implementation: AI Focus Suggestions

## Overview
Phase 4 introduces AI-powered focus block suggestions using Google's Gemini API. Users can generate personalized focus time recommendations based on their calendar events and past focus blocks, then confirm and manage these blocks through a REST API.

## Database Schema
The `focus_blocks` table was created via Flyway migration V3 with the following schema:
```sql
id BIGSERIAL PRIMARY KEY,
user_id BIGINT REFERENCES users(id),
start_time TIMESTAMP NOT NULL,
end_time TIMESTAMP NOT NULL,
status VARCHAR(20) NOT NULL,       -- SCHEDULED | COMPLETED | SKIPPED
ai_confidence_score DOUBLE PRECISION,
created_at TIMESTAMP DEFAULT now()
```

## Files Created

### Entities
- `entity/FocusBlock.java` - JPA entity for focus blocks with Lombok annotations
- `entity/BlockStatus.java` - Enum defining block statuses (SCHEDULED, COMPLETED, SKIPPED)

### DTOs
- `dto/FocusSuggestion.java` - Record for AI-generated suggestions
- `dto/BlockRequest.java` - Record for user block confirmation requests
- `dto/BlockStatusUpdate.java` - Record for status update operations

### Repository
- `repository/FocusBlockRepository.java` - JPA repository with custom query methods

### Services
- `config/AppConfig.java` - Configuration bean for RestTemplate
- `service/PromptBuilder.java` - Builds detailed prompts for Gemini AI
- `service/AiService.java` - Handles HTTP communication with Gemini API
- `service/FocusService.java` - Main business logic for focus operations

### Events
- `event/FocusBlockScheduledEvent.java` - Application event for block scheduling

### Controllers
- `controller/FocusController.java` - REST endpoints for focus operations

### Tests
- `test/service/FocusServiceTest.java` - Unit tests for FocusService

## API Endpoints

All endpoints require JWT authentication and are prefixed with `/api/focus`.

### GET /suggest
Generates AI-powered focus block suggestions for the current user.
- **Response**: `200 OK` with `List<FocusSuggestion>`
- **Logic**: Retrieves today's calendar events and recent completed blocks, builds prompt, calls Gemini API

### POST /block
Confirms and creates a new focus block.
- **Request Body**: `BlockRequest` (start, end, confidence, reason)
- **Response**: `201 Created` with `FocusBlock`
- **Logic**: Saves block with SCHEDULED status, publishes `FocusBlockScheduledEvent`

### GET /blocks
Retrieves all focus blocks for the current user.
- **Response**: `200 OK` with `List<FocusBlock>`

### PATCH /blocks/{id}/status
Updates the status of a specific focus block.
- **Request Body**: `BlockStatusUpdate` (status)
- **Response**: `200 OK` with updated `FocusBlock`
- **Validation**: Verifies block ownership, throws `ResourceNotFoundException` if not found or unauthorized

## Implementation Details

### AI Integration
- Uses Gemini 2.0 Flash model via REST API
- Configured with `@Value("${gemini.api.key}")` - requires API key in `application.properties`
- Request format: JSON with contents/parts/text structure
- Response parsing: Extracts raw JSON from Gemini response, strips markdown fences, deserializes to `List<FocusSuggestion>`

### Prompt Engineering
The prompt includes:
- User's timezone and today's date
- List of today's meetings (title, start, end)
- Recent completed focus blocks from last 2 weeks
- Instructions for 2-4 suggestions of 1-2 hours each
- Emphasis on avoiding meeting conflicts and optimal timing

### Security
- All endpoints protected with JWT authentication
- User context extracted from `SecurityContextHolder`
- Ownership verification for block updates

### Event System
- `FocusBlockScheduledEvent` published when blocks are confirmed
- Ready for Phase 5 reminder scheduling integration

### Validation
- Input validation using `@Valid` and `@NotNull` annotations
- Custom error handling with `AiServiceException` and `ResourceNotFoundException`

## Testing
- Unit tests for `FocusService` with Mockito mocks
- Test coverage includes:
  - Successful suggestion generation
  - Block confirmation with event publishing
  - Error handling for not found blocks
  - Ownership verification for updates

## Configuration
Add to `application.properties`:
```properties
gemini.api.key=your_api_key_here
```

## Dependencies
No new dependencies required - uses existing Spring Boot starters and Jackson for JSON processing.

## Next Steps
Phase 5 will implement reminder scheduling based on the `FocusBlockScheduledEvent` published in this phase.
