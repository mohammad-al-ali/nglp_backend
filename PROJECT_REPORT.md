# 📚 NGLP Backend Project - Full Report & Details

**Project Name:** NGLP Backend v1  
**Type:** Spring Boot REST API with AI Integration  
**Technology Stack:** Java 20 + Spring Boot 4.0.6  
**Status:** Development (Snapshot v0.0.1)  
**Build Tool:** Maven  
**Database:** MySQL  
**Created:** 2026

---

## 📋 Project Overview

**NGLP** is an intelligent educational platform designed to provide AI-powered tutoring and course management. The system supports multiple user roles (Students, Teachers, Admins) and integrates advanced AI capabilities for interactive learning experiences.

### Key Features:
- 🎓 Course and lesson management with video transcripts
- 👨‍🏫 Multi-role user system (Student, Teacher, Admin)
- 🤖 AI-powered intelligent tutor using LLaMA 3.3 70B model
- 💬 Conversation tracking and message history
- 📊 Progress tracking and enrollment management
- 🔐 Spring Security authentication and authorization
- 📝 OpenAPI/Swagger documentation
- 🎬 Video transcript storage and retrieval

---

## 🏗️ Architecture & Structure

```
src/main/java/com/NGLP/backend/v1/
├── Application.java                 # Main Spring Boot entry point
├── DataInitializer.java            # Database seeding (roles initialization)
├── ai/                             # AI & Intelligent Agent
│   ├── AiToolsConfig.java         # AI tools configuration
│   ├── DatabaseChatMemory.java    # Chat memory persistence
│   └── NglpAiAgent.java           # Core AI agent logic
├── controller/                     # REST API Controllers (10 endpoints)
│   ├── AiController.java
│   ├── AuthController.java
│   ├── CategoryController.java
│   ├── ConversationController.java
│   ├── CourseController.java
│   ├── EnrollmentController.java
│   ├── LessonController.java
│   ├── LessonTranscriptController.java
│   ├── RoleController.java
│   └── UserController.java
├── dto/                            # Data Transfer Objects
│   ├── AiAnswer.java
│   ├── AuthRequest.java
│   ├── AuthResponse.java
│   └── ProgressUpdateRequest.java
├── entity/                         # JPA Entities (9 entities)
│   ├── User.java
│   ├── Role.java
│   ├── Course.java
│   ├── Lesson.java
│   ├── Category.java
│   ├── Enrollment.java
│   ├── Conversation.java
│   ├── LessonTranscript.java
│   └── Msg.java
├── exception/                      # Exception Handling
│   └── ResourceNotFoundException.java
├── repo/                           # Spring Data JPA Repositories (9)
│   ├── UserRepo.java
│   ├── RoleRepo.java
│   ├── CourseRepo.java
│   ├── LessonRepo.java
│   ├── CategoryRepo.java
│   ├── EnrollmentRepo.java
│   ├── ConversationRepo.java
│   ├── LessonTranscriptRepo.java
│   └── MsgRepo.java
├── security/                       # Authentication & Authorization
│   └── SecurityConfig.java
└── service/                        # Business Logic Services (10)
    ├── UserService.java
    ├── RoleService.java
    ├── CourseService.java
    ├── LessonService.java
    ├── CategoryService.java
    ├── EnrollmentService.java
    ├── ConversationService.java
    ├── LessonTranscriptService.java
    ├── MsgService.java
    └── FileStorageService.java
```

---

## 📦 Dependencies & Technologies

### Core Framework
| Dependency | Version | Purpose |
|-----------|---------|---------|
| Spring Boot | 4.0.6 | Web framework and application server |
| Spring Data JPA | Latest | ORM and database access |
| Spring Security | Latest | Authentication and authorization |
| Spring MVC | Latest | REST API controllers |

### AI & LLM Integration
| Dependency | Version | Purpose |
|-----------|---------|---------|
| Spring AI | 2.0.0-M4 | AI framework integration |
| Spring AI OpenAI | Latest | LLaMA 3.3 70B model integration via Groq API |
| DatabaseChatMemory | Custom | Persistent conversation memory |

### Database
| Dependency | Version | Purpose |
|-----------|---------|---------|
| MySQL Connector | Latest | MySQL database driver |
| H2 Database | Latest | In-memory database (testing) |

### Utilities & Tools
| Dependency | Version | Purpose |
|-----------|---------|---------|
| Lombok | Latest | Reduce boilerplate code (@Getter, @Setter, etc.) |
| SpringDoc OpenAPI | 2.1.0 | Swagger UI & OpenAPI documentation |
| Validation | Latest | Jakarta Bean Validation |

### Testing
| Dependency | Version | Scope |
|-----------|---------|-------|
| Spring Boot Data JPA Test | Latest | Test scope |
| Spring Boot WebMVC Test | Latest | Test scope |

### Maven Plugins
- **spring-boot-maven-plugin** - Build executable JAR
- **maven-compiler-plugin** - Java compilation with Lombok annotation processing

---

## 🗄️ Database Schema

### Entities & Relationships

#### **User**
```sql
- id (PK)
- fullName
- email
- password (Write-only)
- blocked (boolean)
- role_id (FK to Role)
```

#### **Role**
```sql
- id (PK)
- name (Unique)
- description
```

**Seed Roles:**
- `ROLE_STUDENT` - Student learner account
- `ROLE_TEACHER` - Teacher course creator account
- `ROLE_ADMIN` - Administrator account

#### **Category**
```sql
- id (PK)
- name
- description
```

#### **Course**
```sql
- id (PK)
- title
- description (TEXT)
- category_id (FK to Category)
- teacher_id (FK to User/Teacher)
```

#### **Lesson**
```sql
- id (PK)
- title
- description
- course_id (FK to Course)
- video_url
- duration
```

#### **LessonTranscript**
```sql
- id (PK)
- lesson_id (FK to Lesson)
- timestamp
- content (TEXT)
- speaker
```

#### **Enrollment**
```sql
- id (PK)
- user_id (FK to User/Student)
- course_id (FK to Course)
- enrollmentDate
- completionStatus
```

#### **Conversation**
```sql
- id (PK)
- user_id (FK to User)
- lesson_id (FK to Lesson)
- startTime
- topic
```

#### **Msg**
```sql
- id (PK)
- conversation_id (FK to Conversation)
- sender_id (FK to User)
- message (TEXT)
- timestamp
```

---

## 🔌 API Endpoints Overview

Base URL: `http://localhost:8080`

### Authentication & Users (`/api/users`, `/api/auth`)
- `GET /api/users` - Retrieve all users
- `POST /api/users` - Create new user
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user
- `POST /api/auth/login` - User login
- `POST /api/auth/register` - User registration

### Courses (`/api/courses`)
- `GET /api/courses` - List all courses
- `POST /api/courses` - Create new course
- `PUT /api/courses/{id}` - Update course
- `GET /api/courses/{id}` - Get course details

### Lessons (`/api/lessons`)
- `GET /api/lessons` - List lessons
- `POST /api/lessons` - Create lesson
- `PUT /api/lessons/{id}` - Update lesson
- `GET /api/lessons/{id}/transcripts` - Get lesson transcripts

### AI Tutor (`/api/ai`)
- `POST /api/ai/ask` - Ask AI tutor a question
  - Parameters: `userId`, `lessonId`, `timestamp`, `message`
  - Uses LLaMA 3.3 70B model via Groq API
  - Integrates conversation memory
  - Fetches lesson transcripts for context

### Conversations (`/api/conversations`)
- `GET /api/conversations/{userId}` - User's conversations
- `POST /api/conversations` - Start new conversation
- `GET /api/conversations/{id}/messages` - Get conversation messages

### Enrollments (`/api/enrollments`)
- `POST /api/enrollments` - Enroll in course
- `GET /api/enrollments/{userId}` - User's enrollments
- `PUT /api/enrollments/{id}/progress` - Update progress

### Categories (`/api/categories`)
- `GET /api/categories` - List categories
- `POST /api/categories` - Create category

### Roles (`/api/roles`)
- `GET /api/roles` - List roles

---

## 🤖 AI Features

### LLM Configuration
```properties
Provider: Groq API
Model: llama-3.3-70b-versatile
Base URL: https://api.groq.com/openai
Language: Arabic-optimized prompts
```

### AI Agent (`NglpAiAgent`)
**Purpose:** Intelligent tutor that answers student questions in context

**Features:**
1. **Context-Aware Responses**
   - Uses `fetchLessonTranscript` tool to retrieve lesson content
   - Bases answers strictly on lesson material
   - Maintains conversation memory with `MessageChatMemoryAdvisor`

2. **System Prompt (Arabic)**
   - Responds in clear, friendly, professional Arabic
   - Enforces lesson-focused Q&A
   - Refuses off-topic questions

3. **Memory Management**
   - Persistent chat memory via `DatabaseChatMemory`
   - Conversation tracking in database
   - Message history per conversation

4. **Tool Integration**
   - `fetchLessonTranscript`: Retrieves video transcript by timestamp
   - LLM decides when to use tools based on question
   - Timestamp-aware content retrieval

---

## 🔐 Security Configuration

### Spring Security Features
- User authentication
- Role-based access control (RBAC)
- Three user roles: STUDENT, TEACHER, ADMIN
- Password write-only (excluded from JSON serialization)

### Security Configuration (`SecurityConfig.java`)
- Implements authentication filters
- Endpoint authorization rules
- CORS configuration (if needed)
- User blocking capability

---

## ⚙️ Configuration Properties

### Database
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/nglp_db
spring.datasource.username=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
```

### File Upload
```properties
spring.servlet.multipart.max-file-size=10MB
```

### AI API
```properties
spring.ai.openai.api-key=##############################
spring.ai.openai.base-url=https://api.groq.com/openai
spring.ai.openai.chat.options.model=llama-3.3-70b-versatile
```

---

## 🎯 Core Business Logic

### User Management Service
- CRUD operations on users
- Role assignment
- User blocking/activation
- Password management (TODO: encryption)

### Course Management Service
- Create/update courses
- Assign teachers
- Categorize courses
- List courses by category

### Lesson Management Service
- Create/update lessons
- Associate with courses
- Store lesson transcripts
- Video URL management

### Enrollment Service
- Enroll users in courses
- Track progress
- Completion status
- Generate enrollment reports

### Conversation Service
- Create conversations
- Track conversation metadata
- Message management
- Conversation history

### AI Services
- NglpAiAgent: Main AI tutor logic
- AiToolsConfig: Tool definitions and configuration
- DatabaseChatMemory: Persistent memory for conversations

### File Storage Service
- Handle video uploads (max 10MB)
- Store uploaded files in `uploads/videos/` directory
- Manage file references

---

## 🧪 Testing

### Test Coverage
- Spring Data JPA tests
- Spring MVC (Controller) tests
- Application integration tests

### Test File
```
src/test/java/com/NGLP/backend/v1/ApplicationTests.java
```

---

## 📊 Build & Deployment

### Maven Build
```bash
mvn clean package
```

### Generated Artifacts
- `backend.v1-0.0.1-SNAPSHOT.jar` - Executable JAR
- `backend.v1-0.0.1-SNAPSHOT.jar.original` - Original compiled code

### Application Server
- Embedded Tomcat (Spring Boot)
- Default port: 8080
- Context path: /

### JVM Configuration
- Java 20
- Spring Boot 4.0.6
- Async processing enabled (`@EnableAsync`)

---

## 📚 Documentation

### API Documentation
- **Swagger UI**: Available at `http://localhost:8080/swagger-ui.html`
- **OpenAPI Spec**: Available at `http://localhost:8080/v3/api-docs`
- **Postman Collection**: Referenced in `API.md`

### Project Documentation Files
- `HELP.md` - Maven setup reference
- `API.md` - Detailed API endpoint documentation
- `PROJECT_REPORT.md` - This file

---

## 🚀 Key Technologies Summary

| Component | Technology | Version |
|-----------|-----------|---------|
| **Language** | Java | 20 |
| **Framework** | Spring Boot | 4.0.6 |
| **Database** | MySQL | Latest |
| **ORM** | JPA/Hibernate | Latest |
| **AI/LLM** | Spring AI + Groq | 2.0.0-M4 |
| **API Docs** | Swagger/OpenAPI | 2.1.0 |
| **Build** | Maven | 3.x |
| **Security** | Spring Security | Latest |

---

## 📁 Project Structure Statistics

- **Total Java Files**: 20+ files
- **Entities**: 9 domain models
- **Services**: 10 business logic services
- **Controllers**: 10 REST endpoints
- **Repositories**: 9 data access objects
- **DTOs**: 4 data transfer objects
- **Roles**: 3 (Student, Teacher, Admin)

---

## 🔍 Recent Build Information

**Build Date**: May 6, 2026  
**Build Tool**: Maven  
**Status**: Successfully compiled to `target/classes/`  
**Test Status**: Executed via Surefire

---

## ⚠️ Notable TODOs & Issues

1. **Password Encryption** - Currently passwords are stored in plaintext (TODO in User.java)
2. **API Key Exposure** - Groq API key is hardcoded in `application.properties` (security risk)
3. **Testing** - Limited test coverage, needs more comprehensive tests
4. **Error Handling** - Basic exception handling, could be enhanced

---

## 🎓 Use Cases

### Student Perspective
1. User registers/logs in
2. Browse available courses
3. Enroll in courses
4. Watch lessons with transcripts
5. Ask AI tutor questions about lesson content
6. Track progress and completion

### Teacher Perspective
1. Create courses and lessons
2. Upload lesson videos with transcripts
3. Manage enrolled students
4. Track student progress
5. Assign categories to courses

### Administrator Perspective
1. Manage all users
2. Assign roles
3. Block/unblock users
4. Oversee system performance
5. Monitor AI interactions

---

## 📈 Scalability Considerations

- **Database**: MySQL can handle moderate scale; consider scaling for millions of users
- **AI Model**: Groq API has rate limits; implement caching strategy
- **File Storage**: Current file storage in `uploads/` directory; consider cloud storage (S3, Azure Blob)
- **Concurrency**: `@EnableAsync` enabled for async processing
- **Memory**: Chat memory stored in database; consider distributed cache (Redis)

---

## 🏁 Conclusion

The **NGLP Backend v1** is a well-structured, modern Spring Boot application that combines traditional course management with cutting-edge AI capabilities. The system provides a solid foundation for an intelligent educational platform with multi-user support, comprehensive course management, and an AI-powered tutoring system.

**Maturity Level**: Development/MVP Phase  
**Production Readiness**: Requires security hardening, testing expansion, and performance optimization before production deployment.

---

**Report Generated**: May 22, 2026  
**Project Path**: `d:\ProjectFifthYear\NGLP\backend.v1`
