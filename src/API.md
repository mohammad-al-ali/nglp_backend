NGLP API — Reference (API.md)
Overview  
This document summarizes the available endpoints in the provided Postman collection for the NGLP backend. Base URL used in the collection is http://localhost:8080.

Below are grouped endpoints, HTTP methods, required headers, example request bodies, and example responses extracted from the collection. The examples are minimal, taken directly from the collection payloads and responses.

Users
Base path: /api/users

GET /api/users

Description: Retrieve all users.

Headers: Accept: */*

Response (200):

json
[
{
"id": 6242,
"fullName": "string",
"email": "string",
"password": "string",
"role": { "id": 5980, "name": "string", "description": "string" }
}
]
POST /api/users

Description: Create a new user.

Headers: Content-Type: application/json, Accept: */*

Request body example:

json
{
"id": 4972,
"fullName": "string",
"email": "string",
"password": "string",
"role": { "id": 6781, "name": "string", "description": "string" }
}
Response (200): returns the created user object.

PUT /api/users/:id

Description: Update user by id.

Headers: Content-Type: application/json, Accept: */*

Path variable: :id

Request body: same shape as POST.

Response (200): returns updated user object.

GET /api/users/:id

Description: Get user by id.

Headers: Accept: */*

Response (200): single user object.

DELETE /api/users/:id

Description: Delete user by id.

Response (200): empty body.

Transcripts
Base path: /api/transcripts

GET /api/transcripts

Description: Retrieve all transcripts.

Response (200): array of transcript objects including nested lesson and course structures.

POST /api/transcripts

Headers: Content-Type: application/json, Accept: */*

Request body example:

json
{
"id": 7271,
"lesson": {
"id": 4940,
"title": "string",
"videoUrl": "string",
"durationSeconds": 5870,
"course": {
"id": 1008,
"title": "string",
"description": "string",
"category": { "id": 5280, "name": "string" }
}
},
"startSecond": 7838,
"endSecond": 2308,
"transcriptContent": "string"
}
Response (200): created transcript object.

PUT /api/transcripts/:id — update transcript.

GET /api/transcripts/:id — get transcript by id.

DELETE /api/transcripts/:id — delete transcript by id.

Roles
Base path: /api/roles

GET /api/roles — list roles.

POST /api/roles — create role. Request body:

json
{ "id": 8523, "name": "string", "description": "string" }
PUT /api/roles/:id — update role.

GET /api/roles/:id — get role by id.

DELETE /api/roles/:id — delete role.

Messages
Base path: /api/messages

GET /api/messages — list messages with nested conversation, user, and lesson objects.

POST /api/messages — create message. Request body example includes conversation, senderType, videoTimestamp, content, sentAt.

PUT /api/messages/:id — update message.

GET /api/messages/:id — get message by id.

DELETE /api/messages/:id — delete message.

Lessons
Base path: /api/lessons

GET /api/lessons — list lessons.

POST /api/lessons — create lesson. Example request:

json
{
"id": 4441,
"title": "string",
"videoUrl": "string",
"durationSeconds": 6285,
"course": { "id": 2107, "title": "string", "description": "string" }
}
PUT /api/lessons/:id — update lesson.

GET /api/lessons/:id — get lesson by id.

DELETE /api/lessons/:id — delete lesson.

Courses
Base path: /api/courses

GET /api/courses — list courses.

POST /api/courses — create course. Example:

json
{
"id": 9301,
"title": "string",
"description": "string",
"category": { "id": 2994, "name": "string" }
}
PUT /api/courses/:id — update course.

GET /api/courses/:id — get course by id.

DELETE /api/courses/:id — delete course.

Conversations
Base path: /api/conversations

GET /api/conversations — list conversations.

POST /api/conversations — create conversation. Example request includes user, lesson, and startedAt.

PUT /api/conversations/:id — update conversation.

GET /api/conversations/:id — get conversation by id.

DELETE /api/conversations/:id — delete conversation.

Categories
Base path: /api/categories

GET /api/categories — list categories.

POST /api/categories — create category. Example:

json
{
"id": 8753,
"name": "string",
"parent": { "value": "<Circular reference to Category>" },
"subCategories": [{ "value": "<Circular reference to Category>" }]
}
PUT /api/categories/:id — update category.

GET /api/categories/:id — get category by id.

DELETE /api/categories/:id — delete category.

AI
Base path: /api/ai

POST /api/ai/ask

Description: Send a tutoring/AI question.

Headers: Content-Type: application/json, Accept: */*

Request body example:

json
{
"userId": 4929,
"lessonId": 1116,
"timestamp": "string",
"message": "string",
"conversationId": "string"
}
Response (200): empty object in the collection example.

Common notes and conventions
Headers: Most endpoints use Accept: */*. Endpoints that accept JSON use Content-Type: application/json.

Path variables: The collection uses :id placeholders for resource-specific endpoints. Replace :id with the numeric id.

Responses: The Postman examples show 200 OK for successful operations. Example response bodies are included above for representative endpoints.

Base URL variable: the collection defines baseUrl as http://localhost:8080. Use that as the default base URL when testing locally.

Quick testing checklist
Set Environment variable baseUrl to your server address.

For create/update requests include Content-Type: application/json.

Replace :id in path templates with the actual resource id.

Inspect nested objects (lesson, course, category, conversation) for required fields when creating resources.