# NGLP Backend API Documentation

Base URL: `http://localhost:8080`

## Authentication

All requests use custom header-based auth:

| Header | Value | Required |
|--------|-------|----------|
| `X-User-Id` | User ID (Long) | For role-protected endpoints |
| `X-User-Role` | `ROLE_STUDENT`, `ROLE_TEACHER`, `ROLE_ADMIN` | For role-protected endpoints |

> **Note:** Security is permissive by default (`.anyRequest().permitAll()`). Headers are optional for most endpoints.

---

## Auth API

Base: `POST /api/v1/auth`

### Login

```
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "teacher@nglp.com",
  "password": "teacher123"
}
```

**Response:** `{ "id": 2, "fullName": "...", "email": "...", "role": "ROLE_TEACHER" }`

---

## Quiz API

Base: `GET /api/v1/quizzes`

### 1. Generate AI Quiz (Teacher)

```
POST /api/v1/quizzes/generate
Content-Type: application/json

{
  "lessonId": 1,
  "title": "Quiz Title",
  "numberOfQuestions": 5,
  "teacherId": 2
}
```

Creates a `DRAFT` quiz with AI-generated questions from the lesson transcript. Uses the teacher's preferred LLM provider.

**Response:** `QuizResponse` (full object)

---

### 2. Get Quiz (Teacher/Admin)

```
GET /api/v1/quizzes/{quizId}
```

Returns full quiz with `isCorrect` and `explanation` for all choices.

**Response:** `QuizResponse`

---

### 3. Get Student View

```
GET /api/v1/quizzes/{quizId}/student-view
```

Same quiz without `isCorrect` and `explanation` (hidden from students before submission).

**Response:** `QuizStudentResponse`

---

### 4. List Quizzes by Lesson

```
GET /api/v1/quizzes?lessonId={lessonId}
```

- Teachers: returns all quizzes (DRAFT + PUBLISHED) with answers
- Students: returns only PUBLISHED without answers

**Response:** `List<QuizResponse>`

---

### 5. Add Question (Teacher)

```
POST /api/v1/quizzes/{quizId}/questions
Content-Type: application/json

{
  "questionText": "Question text",
  "difficultyWeight": 5,
  "explanation": "Why this answer is correct",
  "choices": [
    { "choiceText": "A", "isCorrect": true },
    { "choiceText": "B", "isCorrect": false },
    { "choiceText": "C", "isCorrect": false },
    { "choiceText": "D", "isCorrect": false }
  ]
}
```

**Response:** `QuizResponse`

---

### 6. Update Question (Teacher)

```
PUT /api/v1/quizzes/{quizId}/questions/{questionId}
Body: same shape as POST question
```

**Response:** `QuizResponse`

---

### 7. Delete Question (Teacher)

```
DELETE /api/v1/quizzes/{quizId}/questions/{questionId}
```

**Response:** `QuizResponse`

---

### 8. Publish Quiz (Teacher)

```
POST /api/v1/quizzes/{quizId}/publish
```

Changes status from `DRAFT` to `PUBLISHED`. Requires at least one question.

**Response:** `QuizResponse`

---

### 9. Start Attempt (Student)

```
POST /api/v1/quizzes/{quizId}/attempts?studentId={studentId}
```

Attempt number auto-increments (1st attempt = 1, 2nd = 2, etc.).

**Response:** `{ "attemptId": 1, "attemptNumber": 1, "startedAt": "..." }`

---

### 10. Submit Answers & Grade (Student)

```
POST /api/v1/quizzes/attempts/{attemptId}/submit
Content-Type: application/json

{
  "answers": [
    { "questionId": 10, "selectedChoiceId": 100 },
    { "questionId": 11, "selectedChoiceId": 104 }
  ]
}
```

**Grading logic:**
- Correct: `pointsAwarded = difficultyWeight`
- Incorrect: `pointsAwarded = 0`
- `score` = sum of all awarded points

**Response:** `QuizAttemptResponse`
- If `showAnswersAfterSubmit == true`: includes `correctChoiceId`, `correctChoiceText`, `correctChoiceExplanation`
- If `false`: only `isCorrect` per answer

---

### 11. List Attempts

```
GET /api/v1/quizzes/attempts?studentId={studentId}&quizId={quizId}
```

**Response:** `List<QuizAttempt>`

---

## LLM Provider API

Base: `GET /api/v1/llm`

### 1. List Available Providers

```
GET /api/v1/llm/providers
```

**Response:**
```json
[
  {
    "key": "gemini",
    "models": [
      { "key": "gemini-2.5-flash", "name": "Gemini 2.5 Flash", "free": true },
      { "key": "gemini-2.5-pro", "name": "Gemini 2.5 Pro", "free": false }
    ]
  },
  {
    "key": "deepseek",
    "models": [
      { "key": "deepseek-chat", "name": "DeepSeek Chat", "free": true }
    ]
  },
  {
    "key": "groq",
    "models": [
      { "key": "llama-3.3-70b-versatile", "name": "Llama 3.3 70B", "free": true },
      { "key": "mixtral-8x7b-32768", "name": "Mixtral 8x7B", "free": true }
    ]
  }
]
```

Only **enabled** providers appear (those with valid API keys).

---

### 2. Get User Settings

```
GET /api/v1/llm/users/{userId}/settings
```

**Response (no preference saved):**
```json
{ "userId": 2, "providerKey": "", "modelKey": "" }
```

**Response (preference saved):**
```json
{
  "userId": 2,
  "providerKey": "groq",
  "modelKey": "llama-3.3-70b-versatile",
  "updatedAt": "2026-07-24T23:30:00"
}
```

---

### 3. Update User Settings

```
PUT /api/v1/llm/users/{userId}/settings
Content-Type: application/json

{
  "providerKey": "groq",
  "modelKey": "llama-3.3-70b-versatile"
}
```

**Validation:**
- `providerKey` must match an enabled provider
- `modelKey` must be a valid model for that provider

**Response:** same as GET user settings (with `updatedAt`)

**Error:** `400 { "error": "المزود غير متاح: ..." }`

---

## Response Types

### QuizResponse
```json
{
  "id": 1,
  "lessonId": 5,
  "title": "Java Basics Quiz",
  "status": "DRAFT",
  "createdByTeacherId": 2,
  "createdAt": "2026-07-17T12:00:00",
  "showAnswersAfterSubmit": true,
  "questions": [
    {
      "id": 10,
      "questionText": "What is Java?",
      "difficultyWeight": 5,
      "orderIndex": 1,
      "explanation": "Because...",
      "choices": [
        { "id": 100, "choiceText": "A", "isCorrect": true },
        { "id": 101, "choiceText": "B", "isCorrect": false }
      ]
    }
  ]
}
```

### QuizStudentResponse
Same shape as `QuizResponse` but **without** `isCorrect` and `explanation`.

### QuizAttemptResponse
```json
{
  "id": 1,
  "quizId": 1,
  "studentId": 3,
  "attemptNumber": 1,
  "startedAt": "2026-07-17T13:00:00",
  "submittedAt": "2026-07-17T13:05:00",
  "score": 15,
  "answers": [
    {
      "id": 50,
      "questionId": 10,
      "selectedChoiceId": 100,
      "isCorrect": true,
      "pointsAwarded": 5,
      "correctChoiceExplanation": "...",   // only if showAnswersAfterSubmit
      "correctChoiceId": 100,              // only if showAnswersAfterSubmit
      "correctChoiceText": "A"             // only if showAnswersAfterSubmit
    }
  ]
}
```

---

## Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `GEMINI_API_KEY` | Yes | — | Google Gemini API key (free tier available) |
| `DEEPSEEK_API_KEY` | No | — | DeepSeek API key (paid) |
| `GROQ_API_KEY` | No | — | Groq API key (free) |

Alternatively, set keys in `application.properties`:
```properties
spring.ai.google.genai.api-key=your_key
nglp.llm.deepseek.api-key=your_key
nglp.llm.groq.api-key=your_key
```
