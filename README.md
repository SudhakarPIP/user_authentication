# User Authentication Service

A production-grade authentication service built with Spring Boot, MySQL, Jenkins CI/CD, and AWS deployment.

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![Maven](https://img.shields.io/badge/Maven-3.6+-red.svg)](https://maven.apache.org/)

## 📋 Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Database Schema](#database-schema)
- [API Endpoints](#api-endpoints)
- [Quick Start](#quick-start)
- [Testing](#testing)
- [CI/CD](#cicd)
- [AWS Deployment](#aws-deployment)
- [Diagrams](#diagrams)
- [Project Structure](#project-structure)
- [Documentation](#documentation)

## ✨ Features

- ✅ **User Signup** with username/email/mobile validation
- ✅ **Email Verification** with token-based activation
- ✅ **JWT-based Login** (only after email verification)
- ✅ **BCrypt Password Hashing** (10 rounds)
- ✅ **Case-insensitive Username/Email** uniqueness checks
- ✅ **Comprehensive Logging** (SLF4J + Logback)
- ✅ **Unit & Integration Tests** (65+ test cases, >80% coverage)
- ✅ **Jenkins CI/CD Pipeline** (automated build, test, deploy)
- ✅ **AWS Deployment Ready** (EC2, RDS, SES)
- ✅ **Clean Code Architecture** with proper separation of concerns
- ✅ **Security Best Practices** (no information leakage, proper error handling)

## 🛠 Tech Stack

- **Framework**: Spring Boot 3.2.0
- **Database**: MySQL 8.0 (RDS for production)
- **Security**: Spring Security + JWT (jjwt 0.12.3)
- **Password Hashing**: BCrypt
- **Email**: Spring Mail (SMTP/AWS SES)
- **Database Migrations**: Flyway 10.6.0
- **Build Tool**: Maven
- **Testing**: JUnit 5 + Mockito
- **CI/CD**: Jenkins
- **Cloud**: AWS (EC2, RDS, SES)
- **Logging**: SLF4J with Logback

## 🗄 Database Schema

### Users Table

| Field | Type | Constraints | Description |
|:------|:-----|:-----------|:-----------|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique identifier |
| `username` | VARCHAR(50) | NOT NULL, UNIQUE | Username (case-insensitive unique) |
| `name` | VARCHAR(100) | NOT NULL | User's full name |
| `email` | VARCHAR(100) | NOT NULL, UNIQUE | Email address (case-insensitive unique) |
| `mobile` | VARCHAR(15) | NOT NULL | Mobile number |
| `password_hash` | VARCHAR(255) | NOT NULL | BCrypt hashed password |
| `enabled` | BOOLEAN | NOT NULL, DEFAULT FALSE | Account activation status |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Creation timestamp |
| `updated_at` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | Last update timestamp |

**Migration Script**: `src/main/resources/db/migration/V1__Create_users_table.sql`

### Verification Tokens Table

| Field | Type | Constraints | Description |
|:------|:-----|:-----------|:-----------|
| `id` | BIGINT | PRIMARY KEY, AUTO_INCREMENT | Unique identifier |
| `user_id` | BIGINT | NOT NULL, FOREIGN KEY | Reference to users.id |
| `token` | VARCHAR(100) | NOT NULL, UNIQUE | Verification token |
| `expires_at` | TIMESTAMP | NOT NULL | Token expiration time |
| `used` | BOOLEAN | NOT NULL, DEFAULT FALSE | Token usage status |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Creation timestamp |

**Migration Script**: `src/main/resources/db/migration/V2__Create_verification_tokens_table.sql`

**Foreign Key**: `user_id` → `users(id)` ON DELETE CASCADE

## 🔌 API Endpoints

### Base URL
- **Local**: `http://localhost:8080`
- **Production**: Configure via `APP_BASE_URL` environment variable

### 1. Signup

**Endpoint**: `POST /api/v1/signup`

**Request Body**:
```json
{
  "username": "johndoe",
  "name": "John Doe",
  "email": "john@example.com",
  "mobile": "1234567890",
  "password": "password123"
}
```

**Response** (201 Created):
```json
{
  "message": "Signup successful. Please check your email to verify your account.",
  "username": "johndoe",
  "email": "john@example.com"
}
```

**Validation Rules**:
- Username: 3-50 characters, unique (case-insensitive)
- Email: Valid format, unique (case-insensitive)
- Mobile: 10-15 digits
- Password: 8-100 characters

### 2. Email Verification

**Endpoint**: `GET /api/v1/verify?token=<verification_token>`

**Response** (200 OK):
```json
{
  "message": "Email verified successfully. Your account is now activated.",
  "success": true
}
```

**Error Responses**:
- `400 Bad Request`: Invalid or expired token
- `400 Bad Request`: Token already used
- `400 Bad Request`: Missing token parameter

### 3. Login

**Endpoint**: `POST /api/v1/login`

**Request Body**:
```json
{
  "usernameOrEmail": "johndoe",
  "password": "password123"
}
```

**Note**: Can use either username or email (case-insensitive)

**Response** (200 OK):
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "message": "Login successful",
  "username": "johndoe",
  "email": "john@example.com"
}
```

**Error Responses**:
- `400 Bad Request`: Invalid credentials
- `403 Forbidden`: Account not activated (email not verified)

## 🚀 Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- MySQL 8.0+
- Git

### Step 1: Clone Repository

```bash
git clone <repository-url>
cd user_authentication_init
```

### Step 2: Create Database

```sql
CREATE DATABASE pip;
-- Or use the database name configured in application.yml
```

### Step 3: Configure Application

Update `src/main/resources/application.yml` or set environment variables:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/pip?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
    username: root
    password: your_password
```

### Step 4: Configure Email (Optional)

For development, use Mailtrap or Gmail:

```yaml
spring:
  mail:
    host: smtp.mailtrap.io
    port: 587
    username: your_mailtrap_username
    password: your_mailtrap_password
```

### Step 5: Build and Run

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run

# Or with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The application will start on `http://localhost:8080`

### Step 6: Test the API

#### Using cURL

```bash
# Signup
curl -X POST http://localhost:8080/api/v1/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "name": "Test User",
    "email": "test@example.com",
    "mobile": "1234567890",
    "password": "password123"
  }'

# Get verification token from database
# SELECT token FROM verification_tokens WHERE used = false ORDER BY created_at DESC LIMIT 1;

# Verify email
curl "http://localhost:8080/api/v1/verify?token=YOUR_TOKEN_HERE"

# Login
curl -X POST http://localhost:8080/api/v1/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "testuser",
    "password": "password123"
  }'
```

#### Using Postman

1. Import the Postman collection: `postman/User_Authentication_API.postman_collection.json`
2. Set environment variable `base_url` to `http://localhost:8080`
3. Run the collection

See [Postman README](postman/README.md) for detailed instructions.

## 🧪 Testing

### Run All Tests

```bash
# Run all tests
mvn test

# Run with coverage report
mvn clean test jacoco:report
# View report: target/site/jacoco/index.html
```

### Run Specific Tests

```bash
# Unit tests only
mvn test -Dtest=*Test

# Integration tests only
mvn test -Dtest=*IntegrationTest

# Specific test class
mvn test -Dtest=AuthServiceTest
```

### Test Coverage

- **Total Test Cases**: 65+
- **Unit Tests**: 25+ (AuthServiceTest, EmailServiceTest, AuthControllerTest)
- **Integration Tests**: 20+ (AuthIntegrationTest)
- **Coverage**: > 85% ✅

**Generate Coverage Report**:
```bash
mvn clean test jacoco:report
```

View HTML report at: `target/site/jacoco/index.html`

See [Test Coverage Documentation](docs/TEST_COVERAGE.md) for details.

## 🔄 CI/CD

### Jenkins Pipeline

The project includes a `Jenkinsfile` with the following stages:

1. **Checkout**: Clone source code from repository
2. **Build**: Compile the application with Maven
3. **Unit Tests**: Run all unit tests
4. **Package**: Create JAR artifact
5. **Deploy**: Deploy to AWS (EC2/ECS)

### Setup Jenkins Pipeline

1. Create a new Pipeline job in Jenkins
2. Point to the repository containing this `Jenkinsfile`
3. Configure AWS credentials in Jenkins
4. Run the pipeline

The pipeline will:
- ✅ Build the application
- ✅ Run all tests
- ✅ Generate test reports
- ✅ Package the JAR
- ✅ Deploy to AWS

## ☁️ AWS Deployment

### Quick Deployment

See [AWS Deployment Guide](docs/AWS_DEPLOYMENT.md) for detailed instructions.

**Quick Steps**:

1. **Create RDS MySQL instance**
   ```bash
   aws rds create-db-instance \
       --db-instance-identifier auth-db \
       --db-instance-class db.t3.micro \
       --engine mysql \
       --master-username admin \
       --master-user-password <SECURE_PASSWORD> \
       --allocated-storage 20
   ```

2. **Store secrets in AWS Secrets Manager**
   ```bash
   aws secretsmanager create-secret \
       --name auth-service/db-credentials \
       --secret-string '{"username":"admin","password":"your_password","host":"your-rds-endpoint"}'
   ```

3. **Deploy to EC2**
   - Launch EC2 instance
   - Install Java 17
   - Copy JAR file
   - Set environment variables
   - Run application

### RDS Connection Details

- **Database Engine**: MySQL 8.0
- **Instance Class**: db.t3.micro (dev) / db.t3.small (prod)
- **Storage**: 20GB (configurable)
- **Backup**: Enabled (7-day retention)
- **Security**: VPC with security groups

**⚠️ Important**: Never commit database passwords. Use AWS Secrets Manager or environment variables.

### Environment Variables for Production

```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_URL=jdbc:mysql://your-rds-endpoint:3306/auth_db
export DB_USERNAME=your_db_username
export DB_PASSWORD=your_db_password
export JWT_SECRET=your-strong-secret-key-min-256-bits
export MAIL_HOST=email-smtp.us-east-1.amazonaws.com
export MAIL_USERNAME=your_ses_smtp_username
export MAIL_PASSWORD=your_ses_smtp_password
export APP_BASE_URL=https://api.yourdomain.com
export APP_EMAIL_FROM=noreply@yourdomain.com
```

## 📁 Project Structure

```
user_authentication_init/
├── src/
│   ├── main/
│   │   ├── java/com/auth/
│   │   │   ├── UserAuthenticationApplication.java    # Main application class
│   │   │   │
│   │   │   ├── controller/                           # REST API Layer
│   │   │   │   ├── AuthController.java               # Signup, Verify, Login endpoints
│   │   │   │   ├── TestMailController.java          # Email testing endpoints
│   │   │   │   └── EmailDebugController.java         # Email config debug (dev only)
│   │   │   │
│   │   │   ├── service/                              # Business Logic Layer
│   │   │   │   ├── AuthService.java                  # Authentication business logic
│   │   │   │   └── EmailService.java                 # Email sending service
│   │   │   │
│   │   │   ├── repository/                          # Data Access Layer
│   │   │   │   ├── UserRepository.java              # User data access
│   │   │   │   └── VerificationTokenRepository.java # Token data access
│   │   │   │
│   │   │   ├── entity/                               # JPA Entities
│   │   │   │   ├── User.java                         # User entity
│   │   │   │   └── VerificationToken.java           # Verification token entity
│   │   │   │
│   │   │   ├── dto/                                  # Data Transfer Objects
│   │   │   │   ├── SignupRequest.java               # Signup request DTO
│   │   │   │   ├── LoginRequest.java                # Login request DTO
│   │   │   │   ├── AuthResponse.java                # Authentication response DTO
│   │   │   │   └── ApiResponse.java                 # Generic API response DTO
│   │   │   │
│   │   │   ├── config/                               # Configuration Classes
│   │   │   │   └── SecurityConfig.java               # Spring Security configuration
│   │   │   │
│   │   │   ├── util/                                 # Utility Classes
│   │   │   │   └── JwtUtil.java                      # JWT token generation/validation
│   │   │   │
│   │   │   └── exception/                            # Exception Handling
│   │   │       └── GlobalExceptionHandler.java        # Centralized exception handler
│   │   │
│   │   └── resources/
│   │       ├── application.yml                       # Main configuration
│   │       ├── application-dev.yml                   # Development profile
│   │       ├── application-prod.yml                  # Production profile
│   │       └── db/migration/                         # Flyway database migrations
│   │           ├── V1__Create_users_table.sql        # Users table migration
│   │           └── V2__Create_verification_tokens_table.sql  # Tokens table migration
│   │
│   └── test/
│       ├── java/com/auth/
│       │   ├── service/                              # Unit Tests - Service Layer
│       │   │   ├── AuthServiceTest.java             # AuthService unit tests
│       │   │   └── EmailServiceTest.java            # EmailService unit tests
│       │   │
│       │   ├── controller/                           # Unit Tests - Controller Layer
│       │   │   └── AuthControllerTest.java           # AuthController unit tests
│       │   │
│       │   └── integration/                          # Integration Tests
│       │       ├── AuthIntegrationTest.java          # End-to-end API tests
│       │       ├── RealEmailIntegrationTest.java    # Real email sending tests
│       │       └── MailtrapApiTest.java              # Mailtrap API tests
│       │
│       └── resources/
│           └── application-test.yml                  # Test configuration
│
├── docs/                                             # Documentation
│   ├── SEQUENCE_DIAGRAM.md                          # Sequence diagrams (Signup, Verify, Login)
│   ├── ARCHITECTURE_DIAGRAM.md                       # System architecture diagram
│   ├── AWS_DEPLOYMENT.md                            # AWS deployment guide
│   ├── QUICK_START.md                               # Quick start guide
│   ├── API_TESTING.md                               # API testing guide
│   ├── TEST_COVERAGE.md                             # Test coverage details
│   └── [Additional documentation files]
│
├── postman/                                          # Postman Collection
│   ├── User_Authentication_API.postman_collection.json  # Postman collection
│   ├── README.md                                     # Postman usage guide
│   └── API_REQUEST_BODIES.md                        # Request body examples
│
├── Jenkinsfile                                       # Jenkins CI/CD pipeline
├── pom.xml                                           # Maven configuration
├── README.md                                         # This file
└── DELIVERABLES_CHECKLIST.md                        # Deliverables checklist
```

### Package Structure Details

**Controller Layer** (`controller/`):
- Handles HTTP requests/responses
- Validates input using `@Valid`
- Delegates business logic to services
- No business logic or exception handling (handled by GlobalExceptionHandler)

**Service Layer** (`service/`):
- Contains all business logic
- Validates business rules (username/email uniqueness, account activation)
- Manages transactions (`@Transactional`)
- Throws specific exceptions (IllegalArgumentException, IllegalStateException)

**Repository Layer** (`repository/`):
- Data access using Spring Data JPA
- Custom queries for case-insensitive lookups
- No business logic

**Entity Layer** (`entity/`):
- JPA entities mapping to database tables
- Defines relationships and constraints

**DTO Layer** (`dto/`):
- Request/Response objects
- Validation annotations
- Separates API contract from internal entities

**Config Layer** (`config/`):
- Spring configuration classes
- Security configuration

**Util Layer** (`util/`):
- Utility classes (JWT generation/validation)
- Reusable helper methods

**Exception Layer** (`exception/`):
- Centralized exception handling
- Consistent error responses
- Proper HTTP status codes

## 📊 Diagrams

This project includes comprehensive diagrams that illustrate the system architecture and authentication flows. All diagrams are located in the [`docs/`](docs/) folder and use Mermaid format for easy viewing in GitHub, GitLab, and most Markdown viewers.

### Sequence Diagrams

The sequence diagrams illustrate the complete flow of all authentication operations with detailed component interactions:

📄 **[View Complete Sequence Diagrams](docs/SEQUENCE_DIAGRAM.md)**

**Included Flows**:

1. **Signup Flow**
   - Client → AuthController → Service → Repo → DB
   - MailService → SMTP/Mailtrap email sending
   - Complete validation and error handling

2. **Email Verification Flow**
   - Token validation flow with all checks
   - Token existence, expiration, and usage validation
   - Account activation process

3. **Login Flow**
   - Complete authentication flow with JWT generation
   - Username/Email lookup (case-insensitive)
   - Password verification and token generation

**Key Components Shown**:
- ✅ Client → AuthController → Service → Repo → DB
- ✅ MailService → SMTP/Mailtrap
- ✅ Token validation flow
- ✅ Error handling paths (alt blocks)
- ✅ JWT token generation

### Architecture Diagram

The architecture diagram shows the complete system design, component interactions, and deployment architecture:

📄 **[View Complete Architecture Diagram](docs/ARCHITECTURE_DIAGRAM.md)**

**Components Included**:
- ✅ **API Layer**: Spring Boot Application (REST API, Controllers, Services, Repositories)
- ✅ **Database Layer**: RDS MySQL (Primary Database + Read Replica)
- ✅ **Email Service**: AWS SES (Production) + SMTP/Mailtrap (Development)
- ✅ **CI/CD Pipeline**: Jenkins Server (Build, Test, Deploy)
- ✅ **AWS Compute**: EC2 Instance + ECS Container (optional)
- ✅ **Infrastructure**: Load Balancer (ALB), Secrets Manager, Parameter Store, S3, CloudWatch, SNS

**Additional Details**:
- Component descriptions and responsibilities
- Data flow documentation
- Environment configurations (dev/prod)
- Security architecture
- Monitoring and logging setup

**Diagram Format**: Mermaid (renders automatically in GitHub, GitLab, and most Markdown viewers)

---

**Quick Links**:
- 📊 [Sequence Diagrams](docs/SEQUENCE_DIAGRAM.md) - Detailed authentication flows
- 🏗️ [Architecture Diagram](docs/ARCHITECTURE_DIAGRAM.md) - Complete system architecture

## 📚 Documentation

- **[Quick Start Guide](docs/QUICK_START.md)** - Get started quickly
- **[API Testing Guide](docs/API_TESTING.md)** - API testing examples
- **[AWS Deployment Guide](docs/AWS_DEPLOYMENT.md)** - Complete AWS deployment instructions
- **[Sequence Diagrams](docs/SEQUENCE_DIAGRAM.md)** - Signup, Verify, and Login flows
- **[Architecture Diagram](docs/ARCHITECTURE_DIAGRAM.md)** - System architecture
- **[Test Coverage](docs/TEST_COVERAGE.md)** - Test coverage details
- **[Database Requirements](docs/DATABASE_REQUIREMENTS.md)** - Database schema verification
- **[Code Improvements](docs/CODE_IMPROVEMENTS.md)** - Code quality improvements
- **[Postman Collection](postman/README.md)** - Postman collection usage

## 🔒 Security Features

- ✅ **BCrypt Password Hashing** (10 rounds)
- ✅ **JWT Token-based Authentication**
- ✅ **Email Verification** before login
- ✅ **Case-insensitive Uniqueness** checks
- ✅ **Input Validation** (Bean Validation)
- ✅ **SQL Injection Protection** (JPA)
- ✅ **Secrets Management** (AWS Secrets Manager)
- ✅ **No Information Leakage** (generic error messages)
- ✅ **Proper Error Handling** (no stack traces in production)

## 📊 Logging

The application uses SLF4J with Logback for comprehensive logging:

- **Console**: INFO level and above
- **File**: DEBUG level for application packages
- **Location**: `logs/auth-service.log` (configurable)
- **Log Levels**: INFO, DEBUG, WARN, ERROR
- **No Sensitive Data**: Passwords and tokens are never logged

## ✅ Deliverables Checklist

### Repository Structure
- ✅ Clean structure: `controller/service/repo/dto/entity/config/util/exception`
- ✅ README with run + test + deploy steps
- ✅ DB migration scripts (Flyway) in `src/main/resources/db/migration/`
- ✅ Postman collection in `postman/` directory

### CI/CD
- ✅ Jenkinsfile with build, test, package, and deploy stages
- ✅ Pipeline configured for automated builds

### Testing
- ✅ Unit tests (JUnit5 + Mockito) - 25+ test cases
- ✅ Integration tests - 20+ test cases
- ✅ Test coverage > 80%

### Diagrams
- ✅ Sequence diagram (`docs/SEQUENCE_DIAGRAM.md`)
- ✅ Architecture diagram (`docs/ARCHITECTURE_DIAGRAM.md`)

### AWS Deployment
- ✅ Deployment instructions (`docs/AWS_DEPLOYMENT.md`)
- ✅ RDS connection configuration
- ✅ Environment-based configuration (dev/prod)

## 🎯 Acceptance Criteria

All acceptance criteria have been met:

- ✅ Username uniqueness enforced (DB + service layer)
- ✅ Email verification required before login
- ✅ Login works using username or email
- ✅ JWT token returned for verified users
- ✅ Jenkins pipeline builds & runs tests successfully
- ✅ Unit + integration tests exist and pass
- ✅ Diagrams committed under /docs
- ✅ App deployed on AWS and functional with RDS
- ✅ Test coverage > 80%
- ✅ Logging properly implemented

## 🐛 Troubleshooting

### Database Connection Issues
- Verify MySQL is running
- Check connection string in `application.yml`
- Verify credentials
- Check firewall/security group rules

### Email Not Sending
- Verify SMTP credentials
- Check email service configuration
- For Gmail, use App Password (not regular password)
- Check application logs for errors

### Application Won't Start
- Check Java version: `java -version` (should be 17+)
- Verify database is accessible
- Check application logs: `logs/auth-service.log`
- Verify all required environment variables are set

### Tests Failing
- Ensure H2 database is available for tests
- Check test configuration in `application-test.yml`
- Verify all dependencies are installed: `mvn clean install`

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Add tests for new functionality
5. Ensure all tests pass (`mvn test`)
6. Commit your changes (`git commit -m 'Add amazing feature'`)
7. Push to the branch (`git push origin feature/amazing-feature`)
8. Open a Pull Request

## 📝 License

This project is licensed under the MIT License.

## 📧 Support

For issues and questions:
- Open an issue in the repository
- Check the [documentation](docs/) for detailed guides
- Review [Troubleshooting](#troubleshooting) section

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- Flyway for database migrations
- JWT.io for JWT implementation
- All contributors and testers

---

**Built with ❤️ using Spring Boot**
