# PartTime Job Platform

> **Tagline**: *"Find Work. Earn More. Work Safely."*

**PartTime Job** is a modern, production-grade full-stack web application designed to connect college students looking for flexible short-term/part-time employment with verified catering businesses and event organizers needing temporary staff (such as food servers, kitchen assistants, cleaners, and event helpers).

---

## 1. Key Features

- **Multi-Role Authentication & Security**:
  - Role-based Access Control (**STUDENT**, **OWNER**, **ADMIN**) with JWT Bearer tokens and BCrypt password hashing.
  - Suspended user protection (suspended accounts cannot log in, apply, or create jobs).
- **Location & Contact Privacy**:
  - Public job browsing displays only general work areas (e.g., *Kengeri, Bangalore*).
  - Exact venue addresses and organizer contact details are **locked** until the owner formally accepts a student's application.
- **On-Spot Payment & Dispute System**:
  - Built-in workflow for On-Spot payments.
  - Owners mark shifts as `PAID`; students confirm receipt (`CONFIRMED`).
  - Formal complaint module (`PAYMENT_NOT_RECEIVED`, `PAYMENT_PARTIALLY_RECEIVED`, `OWNER_BEHAVIOUR`, `FAKE_JOB`, `WRONG_LOCATION`) with admin resolution workflow.
- **Worker Slot Limits**:
  - Strict limit enforcement prevents over-hiring beyond the required worker count.
  - Automatic status transition to `FILLED` when capacity is reached.
- **Owner Verification**:
  - Verified badges (`VERIFIED`) awarded by platform admins.
  - Safety advisory notices shown to students when viewing jobs from pending/unverified organizers.
- **In-App Notifications**:
  - Real-time notification system for application received, accepted, rejected, cancelled, and payment updates.
- **Audit Logging**:
  - Detailed audit trail tracking all mission-critical events.

---

## 2. Technology Stack

- **Backend**: Node.js, CommonJS, Express, mysql2/promise, bcryptjs, jsonwebtoken, dotenv.
- **Database**: MySQL 8+ (with in-memory H2 profile support for zero-config testing and local demo).
- **Frontend**: HTML5, CSS3 (Custom Design System with Glassmorphism, CSS variables), JavaScript (ES6+ Modules), Bootstrap 5.3.3, Bootstrap Icons.
- **Build Tool**: Apache Maven.

---

## 3. Pre-Seeded Demo Accounts

The application automatically seeds the database with realistic sample data:

| Role | Email | Password | Details |
|---|---|---|---|
| **Admin** | `admin@parttimejob.com` | `Admin@123` | Platform Administrator |
| **Verified Owner** | `owner.srilakshmi@catering.com` | `Owner@123` | Sri Lakshmi Catering Services |
| **Pending Owner** | `owner.royal@catering.com` | `Owner@123` | Royal Grand Events & Catering |
| **Student 1** | `student.pavan@gmail.com` | `Student@123` | Pavan Kumar (BIT Bangalore) |
| **Student 2** | `student.ananya@gmail.com` | `Student@123` | Ananya Sharma (RV College) |

*Note: The login dialog features 1-click quick-fill buttons for instant testing.*

---

## 4. Running the Application Locally

### Node.js + MySQL (current backend)
1. Install Node.js 18+ and MySQL 8+, then create the database: `CREATE DATABASE parttimejob_db;`.
2. Copy `.env.example` to `.env` and set `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, and optional `PORT`/`RUN_SEED`.
3. Install and run:
```bash
npm install
npm start
```
The server automatically executes `src/main/resources/schema.sql` and, when `RUN_SEED=true`, safely executes `data.sql` with duplicate-safe inserts.

### Legacy Java/H2 mode
The original Java implementation remains untouched and can still be run with:
If MySQL is not installed or you want a quick demo:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

Open your browser and navigate to:
```
http://localhost:8080
```

---

## 5. REST API Documentation

### Authentication (`/api/auth`)
- `POST /api/auth/register` - Register a new Student or Owner account.
- `POST /api/auth/login` - Authenticate and receive JWT bearer token.

### Public & Job Discovery (`/api/public/jobs`)
- `GET /api/public/jobs` - Search and filter open jobs (query params: `area`, `location`, `workType`, `jobDate`, `minPayment`, `maxPayment`, `paymentType`).
- `GET /api/public/jobs/recommended` - Get ranked recommendations.
- `GET /api/public/jobs/{id}` - Get job details (with conditional privacy unlocking).

### Student Portal (`/api/student`)
- `GET /api/student/profile` & `PUT /api/student/profile` - Manage student profile.
- `GET /api/student/dashboard` - Get student stats (available, applied, accepted, completed, earnings).
- `POST /api/student/jobs/{jobId}/apply` - Apply for a catering shift.
- `GET /api/student/applications` - List all submitted applications.
- `GET /api/student/applications/accepted` - List accepted shifts with unlocked venues and contact numbers.
- `GET /api/student/applications/completed` - List completed shifts.
- `DELETE /api/student/applications/{id}` - Cancel pending application.
- `PUT /api/student/applications/{id}/confirm-payment` - Confirm receipt of on-spot payment.
- `GET /api/student/payments` - View full payment history.

### Catering Owner Portal (`/api/owner`)
- `GET /api/owner/profile` & `PUT /api/owner/profile` - Manage catering company profile.
- `POST /api/owner/jobs` - Create new catering job post with "On-Spot Payment" checkbox.
- `GET /api/owner/jobs` - List owner's posted jobs.
- `GET /api/owner/jobs/{id}` - Get full job details and applicants list.
- `PUT /api/owner/jobs/{id}` - Update job details.
- `DELETE /api/owner/jobs/{id}` - Cancel job and notify applicants.
- `GET /api/owner/jobs/{id}/applications` - List applicants for specific job.
- `PUT /api/owner/applications/{id}/accept` - Accept applicant (enforces worker limit).
- `PUT /api/owner/applications/{id}/reject` - Reject applicant.
- `PUT /api/owner/applications/{id}/attendance` - Mark worker attendance (`PRESENT`/`ABSENT`).
- `PUT /api/owner/jobs/{id}/complete` - Mark shift as completed.
- `PUT /api/owner/applications/{id}/payment` - Mark payment as `PAID`.
- `GET /api/owner/payments` - List owner payout records.

### Complaints & Reports (`/api/reports`)
- `POST /api/reports` - File dispute/complaint against owner or student.
- `GET /api/reports/my-reports` - View user's filed reports.

### Admin Oversight (`/api/admin`)
- `GET /api/admin/dashboard` - Platform metrics and dispute counts.
- `GET /api/admin/users` - List all registered platform users.
- `PUT /api/admin/owners/{id}/verify` - Grant or revoke verified catering badge.
- `PUT /api/admin/users/{id}/suspend` - Suspend or reactivate user account.
- `GET /api/admin/jobs` - List and moderate all jobs.
- `DELETE /api/admin/jobs/{id}` - Remove fraudulent job.
- `GET /api/admin/reports` - List all platform disputes.
- `PUT /api/admin/reports/{id}/resolve` - Formally resolve dispute with admin remarks.

### Notifications (`/api/notifications`)
- `GET /api/notifications` - Retrieve in-app notifications.
- `GET /api/notifications/unread-count` - Get unread count badge.
- `PUT /api/notifications/{id}/read` - Mark single notification as read.
- `PUT /api/notifications/read-all` - Mark all notifications as read.

---

## 6. Database ER Structure

```text
[User] 1 ──── 1 [StudentProfile] 1 ──── N [JobApplication] N ──── 1 [CateringJob]
  │                                            │                          │
  ├──── 1 [OwnerProfile] 1 ────────────────────┼──────────────────────────┘
  │                                            │
  ├──── N [Report] (as Reporter or Target)     ├──── 1 [PaymentRecord]
  │                                            │
  ├──── N [Notification]                       └──── N [AuditLog]
```
