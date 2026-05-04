# Phase 2 — JWT Authentication System

## Overview

Complete JWT-based authentication system for the Focus Time Optimizer REST API implemented with Spring Boot 3, Spring Security, and JJWT.

## Project Structure

```
src/main/java/com/example/aiFocus/
├── config/
│   └── SecurityConfig.java          # Spring Security configuration
├── controller/
│   └── AuthController.java          # API endpoints for auth
├── dto/
│   ├── RegisterRequest.java         # Registration DTO
│   ├── LoginRequest.java            # Login DTO
│   ├── AuthResponse.java            # Auth response DTO
│   └── ErrorResponse.java           # Error response DTO
├── entity/
│   └── User.java                    # User entity with UserDetails
├── exception/
│   ├── GlobalExceptionHandler.java  # Global exception handler
│   ├── ResourceNotFoundException.java
│   ├── BadCredentialsException.java
│   ├── ConflictException.java
│   └── AiServiceException.java
├── repository/
│   └── UserRepository.java          # User JPA repository
├── security/
│   ├── JwtService.java              # JWT operations
│   └── JwtAuthFilter.java           # JWT validation filter
└── service/
    ├── AuthService.java             # Authentication logic
    └── UserDetailsServiceImpl.java   # Spring Security user details

src/test/java/com/example/aiFocus/
├── controller/
│   └── AuthControllerIntegrationTest.java
├── security/
│   └── JwtServiceTest.java
└── service/
    └── AuthServiceTest.java
```

## Key Features

### 1. **User Entity**
- Implements Spring Security's `UserDetails` interface
- Maps to the `users` table created by Flyway migration V1
- Automatically sets `created_at` via @PrePersist

### 2. **JWT Token Generation**
- Uses JJWT 0.11.5 with HS256 algorithm
- 24-hour expiration time (configurable)
- Bearer token format in Authorization header
- Secret key managed via `app.jwt.secret` property

### 3. **Authentication Flow**

#### Registration
```
POST /api/auth/register
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "password123",
  "timezone": "UTC"
}

Response (201 Created):
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 86400000
}
```

#### Login
```
POST /api/auth/login
{
  "email": "john@example.com",
  "password": "password123"
}

Response (200 OK):
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 86400000
}
```

#### Protected Resources
```
GET /api/your-protected-endpoint
Authorization: Bearer <jwt-token>
```

### 4. **Security Features**
- BCrypt password hashing (strength 10)
- Stateless session policy
- CSRF protection disabled (stateless API)
- JWT filter validates token before each request
- Exception handling with standardized error responses

### 5. **Error Handling**

| Status | Exception | Cause |
|--------|-----------|-------|
| 400 | MethodArgumentNotValidException | Invalid request body/validation errors |
| 401 | BadCredentialsException | Wrong password |
| 404 | ResourceNotFoundException | User not found |
| 409 | ConflictException | Email already registered |
| 500 | Generic Exception | Unexpected errors |

## Configuration

### application.properties
```properties
# JWT Configuration
app.jwt.secret=YourSuperSecretKeyThatIsAtLeast32CharactersLong!
app.jwt.expiration=86400000  # 24 hours in milliseconds
```

### Dependencies
```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
```

## Testing

### JwtServiceTest
Tests JWT token operations:
- `testGenerateToken_returnsNonNullToken()` - Token generation
- `testExtractEmail_returnsCorrectEmail()` - Email extraction
- `testIsTokenValid_returnsTrueForValidToken()` - Valid token validation
- `testIsTokenValid_returnsFalseForExpiredToken()` - Expired token rejection

### AuthServiceTest
Tests authentication logic (with Mockito):
- `testRegister_savesUserAndReturnsToken()` - User registration
- `testLogin_withValidCredentials_returnsToken()` - Valid login
- `testLogin_withWrongPassword_throwsBadCredentialsException()` - Invalid password
- `testLogin_withUnknownEmail_throwsResourceNotFoundException()` - Unknown user

### AuthControllerIntegrationTest
Integration tests with H2 in-memory database:
- `testRegisterEndpoint_returns201AndToken()` - Registration endpoint
- `testLoginEndpoint_withValidCredentials_returns200()` - Login endpoint
- `testLoginEndpoint_withWrongPassword_returns401()` - Wrong password response
- `testProtectedEndpoint_withoutToken_returns403()` - Unauthorized access
- `testProtectedEndpoint_withValidToken_returns200()` - Authorized access
- `testRegisterEndpoint_withDuplicateEmail_returns409()` - Email conflict
- `testLoginEndpoint_withUnknownEmail_returns404()` - User not found

## Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=JwtServiceTest
mvn test -Dtest=AuthServiceTest
mvn test -Dtest=AuthControllerIntegrationTest

# Run specific test method
mvn test -Dtest=JwtServiceTest#testGenerateToken_returnsNonNullToken
```

## Best Practices Implemented

✅ Constructor injection (no @Autowired on fields)
✅ Comprehensive Javadoc documentation
✅ Spring Security's UserDetails implementation
✅ BCryptPasswordEncoder for secure password storage
✅ Stateless authentication (no sessions)
✅ Global exception handling
✅ Input validation with @Valid
✅ H2 in-memory database for integration tests
✅ Mock-based unit tests with Mockito
✅ Standard Spring Boot 3 package structure

## Next Steps

1. Implement refresh token mechanism
2. Add role-based authorization (RBAC)
3. Add audit logging for authentication events
4. Implement rate limiting on auth endpoints
5. Add OAuth2 integration (Google, GitHub)

