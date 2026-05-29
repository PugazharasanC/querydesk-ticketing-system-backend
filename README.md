# QueryDesk Backend

Multi-Team Customer Support & Ticketing Platform built with Spring Boot.

This backend powers a real-time enterprise-style ticketing system with:
- JWT Authentication
- Role-Based Access Control (RBAC)
- Ticket Workflows
- Team Management
- Escalation Handling
- Internal Notes
- Real-Time Notifications (SSE)
- Dashboard Analytics
- Audit Trail Tracking

---

# Features

## Authentication & Security
- JWT Authentication
- Role-Based Authorization
- Protected APIs using Spring Security
- Password Reset via Email
- Account Enable/Disable Support

## Ticket Management
- Create Tickets
- Update Ticket Status
- Assign Tickets to Agents
- Self-Assign Tickets
- Escalate Tickets
- Search & Filter Tickets
- Dashboard Statistics

## Comments & Internal Notes
- Public comments visible to customers
- Internal notes visible only to agents/managers/admins
- Secure backend filtering for internal notes

## Team Management
- Create Teams
- Assign Team Members
- Manager Assignment
- Multi-Team Ticket Workflow

## Notifications
- Real-time notifications using SSE (Server-Sent Events)
- Unread Notification Count
- Ticket Assignment Notifications
- Escalation Notifications
- Comment Notifications

## Audit Trail
Tracks:
- Ticket creation
- Assignment
- Escalation
- Status changes
- Resolution actions

---

# Tech Stack

- Java 21
- Spring Boot 3
- Spring Security
- Spring Data MongoDB
- MongoDB
- Maven
- JWT
- SSE (Server-Sent Events)
- Swagger / OpenAPI
- Lombok

---

# Architecture

```text
Frontend
   ↓
REST APIs
   ↓
Controller
   ↓
Service
   ↓
Repository
   ↓
MongoDB
```

## Layered Architecture

```text
Controller → Service → Repository → Database
DTO ↔ Mapper ↔ Entity
```

---

# Roles & Permissions

| Role     | Permissions             |
|----------|-------------------------|
| CUSTOMER | Create/view own tickets |
| AGENT    | Handle assigned tickets |
| MANAGER  | Assign/escalate tickets |
| ADMIN    | Full system access      |

---

# Ticket Lifecycle

```text
OPEN
  ↓
IN_PROGRESS
  ↓
ESCALATED
  ↓
RESOLVED
  ↓
CLOSED
```

---

# Internal Notes

Agents, managers, and admins can create internal notes.

Customers cannot view internal notes.

Internal notes are filtered securely at backend level.

---

# Real-Time Notifications

SSE (Server-Sent Events) are used for:
- live notifications
- unread count updates
- ticket updates
- assignment updates
- escalation events

---

# API Documentation

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

---

# Main API Modules

- Authentication APIs
- User Management APIs
- Ticket APIs
- Team APIs
- Notification APIs
- SSE APIs
- Profile APIs

---

# Environment Variables

Create an `.env` or configure `application.properties`:

```properties
SPRING_DATA_MONGODB_URI=
JWT_SECRET=
MAIL_USERNAME=
MAIL_PASSWORD=
FRONTEND_URL=
```

---

# Run Locally

## Clone Repository

```bash
git clone <repo-url>
cd backend
```

## Install Dependencies

```bash
mvn clean install
```

## Run Application

```bash
mvn spring-boot:run
```

---

# API Base URL

```text
http://localhost:8080
```

---

# Future Improvements

- SLA Automation
- WebSocket Support
- File Attachments
- Team-Based Analytics
- Docker Deployment
- Kubernetes Deployment
- Event-Driven Architecture
- Redis Caching

---

# Author

Developed as a full-stack enterprise-style support ticketing platform using Spring Boot and MongoDB.