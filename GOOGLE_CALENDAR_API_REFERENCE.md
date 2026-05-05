# Google Calendar Integration - API Reference

## Base URL
```
http://localhost:8081/api/calendar
```

## Authentication
All endpoints require JWT authentication in the `Authorization` header, except `/oauth2/callback`:
```
Authorization: Bearer {JWT_TOKEN}
```

---

## Endpoints

### 1. Connect Google Calendar
**Endpoint**: `GET /connect`  
**Auth**: Required ✅  
**Description**: Get the Google OAuth authorization URL

**Response** (200 OK):
```json
{
  "authUrl": "https://accounts.google.com/o/oauth2/v2/auth?client_id=...&redirect_uri=...&scope=..."
}
```

**Error** (401 Unauthorized):
```json
{
  "status": 401,
  "message": "Unauthorized"
}
```

---

### 2. Google OAuth Callback
**Endpoint**: `GET /oauth2/callback?code={authorization_code}`  
**Auth**: Not Required ❌ (permitAll)  
**Description**: OAuth callback - exchanges code for token and redirects to sync

**Redirect**: 302 Found → `/api/calendar/sync`  
**On Error**: 302 Found → `/api/calendar/error`

**Usage**:
1. User clicks the `authUrl` from endpoint #1
2. User authorizes the application
3. Google redirects to this endpoint with authorization code
4. System exchanges code for access token
5. System redirects to `/sync` endpoint

---

### 3. Sync Google Calendar
**Endpoint**: `GET /sync`  
**Auth**: Required ✅  
**Description**: Sync events from Google Calendar (today to today+7 days)

**Response** (200 OK):
```json
{
  "syncedCount": 5,
  "events": [
    {
      "id": 1,
      "title": "Team Meeting",
      "startTime": "2026-05-05T10:00:00",
      "endTime": "2026-05-05T11:00:00",
      "type": "MEETING",
      "source": "GOOGLE"
    },
    {
      "id": 2,
      "title": "Lunch",
      "startTime": "2026-05-05T12:00:00",
      "endTime": "2026-05-05T13:00:00",
      "type": "MEETING",
      "source": "GOOGLE"
    }
  ]
}
```

**Error** (404 Not Found) - No Google token connected:
```json
{
  "status": 404,
  "message": "Google Calendar not connected for this user. Please connect first."
}
```

---

### 4. Get Today's Events
**Endpoint**: `GET /today`  
**Auth**: Required ✅  
**Description**: Get all calendar events for today

**Response** (200 OK):
```json
[
  {
    "id": 1,
    "title": "Team Meeting",
    "startTime": "2026-05-05T10:00:00",
    "endTime": "2026-05-05T11:00:00",
    "type": "MEETING",
    "source": "GOOGLE"
  },
  {
    "id": 3,
    "title": "Focus Block",
    "startTime": "2026-05-05T14:00:00",
    "endTime": "2026-05-05T15:30:00",
    "type": "FOCUS",
    "source": "MANUAL"
  }
]
```

---

### 5. Get Events in Date Range
**Endpoint**: `GET /events?from={yyyy-MM-dd}&to={yyyy-MM-dd}`  
**Auth**: Required ✅  
**Description**: Get events within a date range

**Query Parameters**:
- `from` (required): Start date in format YYYY-MM-DD
- `to` (required): End date in format YYYY-MM-DD

**Example**:
```
GET /api/calendar/events?from=2026-05-01&to=2026-05-31
```

**Response** (200 OK):
```json
[
  {
    "id": 1,
    "title": "May 5 Meeting",
    "startTime": "2026-05-05T10:00:00",
    "endTime": "2026-05-05T11:00:00",
    "type": "MEETING",
    "source": "GOOGLE"
  },
  {
    "id": 10,
    "title": "May 10 Conference",
    "startTime": "2026-05-10T09:00:00",
    "endTime": "2026-05-10T17:00:00",
    "type": "MEETING",
    "source": "GOOGLE"
  }
]
```

---

### 6. Get Free Time Slots
**Endpoint**: `GET /free-slots?date={yyyy-MM-dd}`  
**Auth**: Required ✅  
**Description**: Get available time slots for a specific date (08:00-20:00, min 30 min)

**Query Parameters**:
- `date` (required): Date in format YYYY-MM-DD

**Example**:
```
GET /api/calendar/free-slots?date=2026-05-05
```

**Response** (200 OK):
```json
[
  {
    "startTime": "2026-05-05T08:00:00",
    "endTime": "2026-05-05T10:00:00",
    "durationMinutes": 120
  },
  {
    "startTime": "2026-05-05T11:00:00",
    "endTime": "2026-05-05T12:00:00",
    "durationMinutes": 60
  },
  {
    "startTime": "2026-05-05T13:00:00",
    "endTime": "2026-05-05T14:00:00",
    "durationMinutes": 60
  }
]
```

---

### 7. Create Manual Event
**Endpoint**: `POST /events/manual`  
**Auth**: Required ✅  
**Content-Type**: `application/json`  
**Description**: Create a manually added calendar event

**Request Body**:
```json
{
  "title": "Focus Block",
  "startTime": "2026-05-05T14:00:00",
  "endTime": "2026-05-05T15:30:00",
  "type": "FOCUS"
}
```

**Event Types**: `MEETING`, `FOCUS`, `BREAK`, `OTHER`

**Response** (201 Created):
```json
{
  "id": 5,
  "title": "Focus Block",
  "startTime": "2026-05-05T14:00:00",
  "endTime": "2026-05-05T15:30:00",
  "type": "FOCUS",
  "source": "MANUAL"
}
```

**Error** (400 Bad Request) - Validation failed:
```json
{
  "status": 400,
  "message": "Event title is required"
}
```

**Error** (401 Unauthorized):
```json
{
  "status": 401,
  "message": "Unauthorized"
}
```

---

## Complete API Usage Flow

### Step 1: Connect Google Calendar
```bash
curl -X GET http://localhost:8081/api/calendar/connect \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

Response:
```json
{
  "authUrl": "https://accounts.google.com/o/oauth2/v2/auth?..."
}
```

### Step 2: User Authorizes
- Click the `authUrl`
- User grants permission
- Redirected back to `/oauth2/callback?code=...`

### Step 3: Sync Google Calendar
```bash
curl -X GET http://localhost:8081/api/calendar/sync \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

### Step 4: Get Today's Events
```bash
curl -X GET http://localhost:8081/api/calendar/today \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

### Step 5: Find Free Slots
```bash
curl -X GET "http://localhost:8081/api/calendar/free-slots?date=2026-05-05" \
  -H "Authorization: Bearer {JWT_TOKEN}"
```

### Step 6: Add Manual Event
```bash
curl -X POST http://localhost:8081/api/calendar/events/manual \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Focus Block",
    "startTime": "2026-05-05T14:00:00",
    "endTime": "2026-05-05T15:30:00",
    "type": "FOCUS"
  }'
```

---

## Event Types

| Type | Purpose |
|------|---------|
| `MEETING` | Official meetings or appointments |
| `FOCUS` | Focus/work blocks |
| `BREAK` | Break or rest periods |
| `OTHER` | Other types of events |

---

## Event Sources

| Source | Description |
|--------|-------------|
| `GOOGLE` | Synced from Google Calendar |
| `MANUAL` | Created manually by user |

---

## HTTP Status Codes

| Status | Meaning |
|--------|---------|
| 200 | OK - Request successful |
| 201 | Created - Resource created successfully |
| 400 | Bad Request - Invalid parameters |
| 401 | Unauthorized - Missing or invalid JWT token |
| 403 | Forbidden - Access denied |
| 404 | Not Found - Resource not found |
| 500 | Internal Server Error |

---

## Error Handling

### Missing Required Parameter
```json
{
  "status": 400,
  "message": "Required parameter missing"
}
```

### Invalid Date Format
```json
{
  "status": 400,
  "message": "Invalid date format. Use YYYY-MM-DD"
}
```

### Google Calendar Not Connected
```json
{
  "status": 404,
  "message": "Google Calendar not connected for this user. Please connect first."
}
```

---

## Example: Complete Workflow (Using cURL)

```bash
# 1. Login and get JWT token
JWT=$(curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }' | jq -r '.token')

# 2. Get Google authorization URL
curl -X GET http://localhost:8081/api/calendar/connect \
  -H "Authorization: Bearer $JWT"

# 3. [User visits URL and authorizes in browser]

# 4. Sync Google Calendar
curl -X GET http://localhost:8081/api/calendar/sync \
  -H "Authorization: Bearer $JWT"

# 5. Get today's events
curl -X GET http://localhost:8081/api/calendar/today \
  -H "Authorization: Bearer $JWT"

# 6. Find free slots for today
curl -X GET "http://localhost:8081/api/calendar/free-slots?date=$(date +%Y-%m-%d)" \
  -H "Authorization: Bearer $JWT"

# 7. Add a manual focus block
curl -X POST http://localhost:8081/api/calendar/events/manual \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Deep Work Session",
    "startTime": "2026-05-05T14:00:00",
    "endTime": "2026-05-05T15:30:00",
    "type": "FOCUS"
  }'
```

---

## Notes

1. **DateTime Format**: All datetime values use ISO 8601 format: `YYYY-MM-DDTHH:MM:SS`
2. **Time Zone**: Server uses system default timezone
3. **Work Hours**: Free slots are computed between 08:00 and 20:00
4. **Minimum Slot Duration**: Free slots must be at least 30 minutes long
5. **Event Deduplication**: Duplicate events from Google are automatically detected and skipped
6. **All-Day Events**: All-day events from Google Calendar are skipped (not synced)
7. **Token Storage**: Google access tokens are stored in memory (not persistent)

---

For more information, see `PHASE_3_IMPLEMENTATION.md`

