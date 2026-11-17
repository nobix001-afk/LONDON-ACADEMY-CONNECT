# London Academy Connect — Architecture & Database Schema Specification

## 1. System Architecture Overview

London Academy Connect follows a reactive, clean, layered **MVVM (Model-View-ViewModel)** and **Repository pattern** with offline-first local persistence powered by **Room Database** and direct synchronization mappings to **Google Cloud Firestore**.

```
               ┌────────────────────────────────────────────────────────┐
               │              Jetpack Compose Presentation Layer        │
               │  (Login, Admin, Sub-Admin, Employee, Student Screens)  │
               └───────────────────────────┬────────────────────────────┘
                                           │ StateFlow / Events
                                           ▼
               ┌────────────────────────────────────────────────────────┐
               │                      EduViewModel                      │
               │  - Role State Machine & Session Dispatcher             │
               │  - GPS Geofence & Haversine Distance Engine            │
               │  - Mock GPS Rejection & Camera Selfie Handler          │
               │  - Dynamic Working Days Calculator (from DOJ)          │
               │  - Fee Ledger & Automated Balance Recalculator         │
               └───────────────────────────┬────────────────────────────┘
                                           │
                                           ▼
               ┌────────────────────────────────────────────────────────┐
               │                     EduRepository                      │
               │  - Multi-tenant CRUD operations                        │
               │  - Immutable Audit Logging Pipeline                    │
               │  - Pre-seeded Realistic Institutional Fixtures         │
               └───────────────┬────────────────────────┬───────────────┘
                               │                        │
                               ▼                        ▼
        ┌─────────────────────────────┐   ┌─────────────────────────────┐
        │        Local Storage        │   │       Cloud Firestore       │
        │   (Room Database + SQLite)  │   │   (Production Cloud DB)     │
        └─────────────────────────────┘   └─────────────────────────────┘
```

---

## 2. Cloud Firestore Database Schema

### Collection: `users`
- **Document ID**: `userId` (e.g. `admin`, `subadmin1`, `trainer1`, `student1`)
- **Fields**:
  - `userId` (string, unique)
  - `password` (string, encrypted)
  - `name` (string)
  - `role` (string: `ADMIN` | `SUB_ADMIN` | `EMPLOYEE` | `STUDENT`)
  - `contactNo` (string)
  - `email` (string)
  - `isActive` (boolean)
  - `createdAt` (timestamp)

### Collection: `employees`
- **Document ID**: `employeeId`
- **Fields**:
  - `employeeId` (string, unique)
  - `name` (string)
  - `contactNo` (string)
  - `email` (string)
  - `department` (string: e.g. `Teaching Staff`, `Digital Marketing Trainer`, `Web Development Trainer`, `Office Boy`, `Cleaning Staff`)
  - `dateOfJoining` (string: `YYYY-MM-DD`)
  - `canManageStudents` (boolean)
  - `canViewFees` (boolean)
  - `canMarkAttendance` (boolean)
  - `photoUri` (string)

### Collection: `students`
- **Document ID**: `admissionNo` (e.g. `STU-2024-001`)
- **Fields**:
  - `admissionNo` (string, unique)
  - `name` (string)
  - `contactNo` (string)
  - `address` (string)
  - `courseName` (string)
  - `batchName` (string)
  - `dateOfJoining` (string: `YYYY-MM-DD`)
  - `courseDuration` (string)
  - `totalFees` (number)
  - `paidFees` (number)
  - `pendingFees` (number: `totalFees - paidFees`)
  - `paymentPlan` (string: `ONE-TIME` | `MONTHLY` | `PART PAYMENT` | `YEARLY`)
  - `assignedTeacherId` (string, references `employees.employeeId`)
  - `assignedTeacherName` (string)
  - `userId` (string, references `users.userId`)
  - `nextDueDate` (string)
  - `monthlyFeeAmount` (number)
  - `status` (string: `ACTIVE` | `COMPLETED` | `INACTIVE`)

### Collection: `employee_attendance`
- **Document ID**: Auto-generated
- **Fields**:
  - `employeeId` (string)
  - `employeeName` (string)
  - `department` (string)
  - `date` (string: `YYYY-MM-DD`)
  - `time` (string: `HH:mm:ss`)
  - `status` (string: `PRESENT` | `ABSENT` | `HALF_DAY` | `LEAVE`)
  - `latitude` (number)
  - `longitude` (number)
  - `distanceMeters` (number, distance from office coordinates)
  - `selfieUri` (string, storage URL or encrypted file path)
  - `isMockLocation` (boolean, rejects spoofed GPS)
  - `deviceModel` (string)

### Collection: `student_attendance`
- **Document ID**: Auto-generated
- **Fields**:
  - `admissionNo` (string)
  - `studentName` (string)
  - `courseName` (string)
  - `batchName` (string)
  - `date` (string: `YYYY-MM-DD`)
  - `status` (string: `PRESENT` | `ABSENT` | `HALF_DAY` | `LEAVE`)
  - `markedByUserId` (string)

### Collection: `fee_payments`
- **Document ID**: Auto-generated
- **Fields**:
  - `receiptNo` (string, e.g. `REC-100231`)
  - `admissionNo` (string)
  - `studentName` (string)
  - `amount` (number)
  - `paymentDate` (string: `YYYY-MM-DD`)
  - `paymentMode` (string: `UPI` | `Cash` | `Card` | `Bank Transfer`)
  - `paymentPlan` (string)
  - `nextDueDate` (string)
  - `collectedBy` (string)
  - `remarks` (string)

### Collection: `notifications`
- **Document ID**: Auto-generated
- **Fields**:
  - `title` (string)
  - `message` (string)
  - `category` (string: `Offer` | `Discount` | `Holiday` | `Office Update` | `Fee Reminder` | `Announcement` | `Event`)
  - `targetType` (string: `ALL_STUDENTS` | `ALL_EMPLOYEES` | `COURSE` | `BATCH` | `TEACHER` | `SUB_ADMIN` | `INDIVIDUAL`)
  - `targetValue` (string)
  - `date` (string)
  - `time` (string)
  - `sentBy` (string)

### Collection: `holidays`
- **Document ID**: Auto-generated
- **Fields**:
  - `holidayName` (string)
  - `date` (string: `YYYY-MM-DD`)
  - `description` (string)

### Collection: `office_settings`
- **Document ID**: `campus_config`
- **Fields**:
  - `officeName` (string)
  - `latitude` (number)
  - `longitude` (number)
  - `allowedRadiusMeters` (number, default: 10.0)
  - `weeklyOff` (string: `SUNDAY` | `SAT_SUN`)

### Collection: `audit_logs`
- **Document ID**: Auto-generated
- **Fields**:
  - `userId` (string)
  - `role` (string)
  - `action` (string)
  - `details` (string)
  - `timestamp` (number)
  - `date` (string)
  - `time` (string)
  - `deviceInfo` (string)
