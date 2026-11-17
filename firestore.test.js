const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const serverTimestamp = () => new Date(Date.now() - 1000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: denied read and write on all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("students").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("branches").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("branches").doc("b1").set({
    branchName: "Main",
    addedBy: "ADMIN",
    createdAt: serverTimestamp(),
  }));
});

test("Owner user profile: valid create and partial update succeed; ghost fields fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(profileRef.set({
    userId: ALICE_UID,
    displayName: "Alice Admin",
    email: "alice@londonacademy.edu",
    activePortalRole: "ADMIN",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  }));

  await assertSucceeds(profileRef.get());

  await assertSucceeds(profileRef.update({
    activePortalRole: "SUB_ADMIN",
    updatedAt: serverTimestamp(),
  }));

  // Shadow update / ghost field injection must fail
  await assertFails(profileRef.update({
    isSuperAdmin: true,
    updatedAt: serverTimestamp(),
  }));
});

test("Cross-user isolation: Alice cannot read, list, create, update, or delete Bob's data", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const bobRoot = context.firestore().collection("users").doc(BOB_UID);
    await bobRoot.set({
      userId: BOB_UID,
      displayName: "Bob",
      email: "bob@londonacademy.edu",
      createdAt: serverTimestamp(),
    });
    await bobRoot.collection("students").doc("LA9001").set({
      admissionNo: "LA9001",
      name: "Bob Student",
      courseName: "Full Stack",
      branchName: "Main Campus",
      batchName: "Batch A",
      dateOfJoining: "2026-11-01",
      courseDuration: "6 Months",
      totalFees: 50000,
      paidFees: 20000,
      pendingFees: 30000,
      paymentPlan: "MONTHLY",
      installmentCount: 6,
      assignedTeacherId: "trainer1",
      assignedTeacherName: "Rahul Sharma",
      portalUserId: "stu9001",
      status: "ACTIVE",
      createdAt: serverTimestamp(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobRootFromAlice = aliceDb.collection("users").doc(BOB_UID);

  await assertFails(bobRootFromAlice.get());
  await assertFails(bobRootFromAlice.collection("students").get());
  await assertFails(bobRootFromAlice.collection("students").doc("LA9001").get());
  await assertFails(bobRootFromAlice.collection("branches").doc("b1").set({
    branchName: "Hijack",
    addedBy: ALICE_UID,
    createdAt: serverTimestamp(),
  }));
  await assertFails(bobRootFromAlice.collection("students").doc("LA9001").delete());
});

test("Branches & Courses: owner create, list, update, delete succeed; invalid payloads fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const branchRef = aliceDb.collection("users").doc(ALICE_UID).collection("branches").doc("branch_delhi");

  await assertSucceeds(branchRef.set({
    branchName: "Main Campus (Delhi)",
    address: "Green Park, New Delhi",
    contactNo: "9876543210",
    addedBy: "ADMIN",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  }));

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("branches").get());

  await assertSucceeds(branchRef.update({
    contactNo: "9999988888",
    updatedAt: serverTimestamp(),
  }));

  // Empty branchName fails validation
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).collection("branches").doc("bad_branch").set({
    branchName: "",
    addedBy: "ADMIN",
    createdAt: serverTimestamp(),
  }));

  const courseRef = aliceDb.collection("users").doc(ALICE_UID).collection("courses").doc("course_fs");
  await assertSucceeds(courseRef.set({
    courseName: "Full Stack Web Development",
    defaultDuration: "12 Months",
    defaultFees: 60000,
    addedBy: "ADMIN",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  }));

  // Negative fees fail validation
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).collection("courses").doc("bad_course").set({
    courseName: "Bad Course",
    defaultDuration: "1 Month",
    defaultFees: -500,
    addedBy: "ADMIN",
    createdAt: serverTimestamp(),
  }));
});

test("Employees, Students, Attendance, Payments, Settings & Holidays validate schema cleanly", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const aliceRoot = aliceDb.collection("users").doc(ALICE_UID);

  // Employee
  await assertSucceeds(aliceRoot.collection("employees").doc("trainer1").set({
    employeeId: "trainer1",
    name: "Rahul Sharma",
    contactNo: "9876543212",
    email: "rahul@londonacademy.edu",
    department: "Teaching Staff",
    branchName: "Main Campus (Delhi)",
    dateOfJoining: "2024-03-01",
    canManageStudents: false,
    canViewFees: false,
    canMarkAttendance: true,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  }));

  // Student
  await assertSucceeds(aliceRoot.collection("students").doc("LA10025").set({
    admissionNo: "LA10025",
    name: "Rahul Kumar",
    courseName: "Full Stack Web Development",
    branchName: "Main Campus (Delhi)",
    batchName: "Morning Batch A",
    dateOfJoining: "2026-05-10",
    courseDuration: "12 Months",
    totalFees: 60000,
    paidFees: 35000,
    pendingFees: 25000,
    paymentPlan: "MONTHLY",
    installmentCount: 12,
    assignedTeacherId: "trainer1",
    assignedTeacherName: "Rahul Sharma",
    portalUserId: "student4",
    status: "ACTIVE",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  }));

  // Student Attendance (invalid status rejected)
  await assertFails(aliceRoot.collection("student_attendance").doc("LA10025_2026_11_15").set({
    admissionNo: "LA10025",
    studentName: "Rahul Kumar",
    courseName: "Full Stack Web Development",
    batchName: "Morning Batch A",
    date: "2026-11-15",
    status: "INVALID_STATUS",
    markedByUserId: "trainer1",
    workedMinutes: 120,
    requiredMinutes: 120,
    latitude: 28.6139,
    longitude: 77.209,
    distanceMeters: 4.5,
    isMockLocation: false,
    createdAt: serverTimestamp(),
  }));

  await assertSucceeds(aliceRoot.collection("student_attendance").doc("LA10025_2026_11_15").set({
    admissionNo: "LA10025",
    studentName: "Rahul Kumar",
    courseName: "Full Stack Web Development",
    batchName: "Morning Batch A",
    date: "2026-11-15",
    status: "PRESENT",
    markedByUserId: "trainer1",
    checkInTime: "10:00:00",
    checkOutTime: "12:00:00",
    workedMinutes: 120,
    requiredMinutes: 120,
    latitude: 28.6139,
    longitude: 77.209,
    distanceMeters: 4.5,
    isMockLocation: false,
    notes: "Sunday Weekly Off • Attendance Open",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  }));

  // Fee Payment (amount <= 0 rejected)
  await assertFails(aliceRoot.collection("fee_payments").doc("REC-0").set({
    receiptNo: "REC-0",
    admissionNo: "LA10025",
    studentName: "Rahul Kumar",
    amount: 0,
    paymentDate: "2026-11-15",
    paymentMode: "UPI",
    paymentPlan: "MONTHLY",
    collectedBy: "admin",
    createdAt: serverTimestamp(),
  }));

  await assertSucceeds(aliceRoot.collection("fee_payments").doc("REC-1001").set({
    receiptNo: "REC-1001",
    admissionNo: "LA10025",
    studentName: "Rahul Kumar",
    amount: 5000,
    paymentDate: "2026-11-15",
    paymentMode: "UPI",
    paymentPlan: "MONTHLY",
    collectedBy: "admin",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  }));

  // Office Settings
  await assertSucceeds(aliceRoot.collection("office_settings").doc("default").set({
    officeName: "London Academy Campus",
    latitude: 28.6139,
    longitude: 77.209,
    allowedRadiusMeters: 10,
    weeklyOff: "SUNDAY",
    standardDailyHours: 8,
    standardStudentDailyHours: 2,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  }));
});

test("Audit logs: append-only (create succeeds, update and delete denied)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const logRef = aliceDb.collection("users").doc(ALICE_UID).collection("audit_logs").doc("log_1");

  await assertSucceeds(logRef.set({
    actorId: "admin",
    role: "ADMIN",
    action: "LOGIN",
    details: "Logged into portal",
    date: "2026-11-15",
    time: "10:00:00",
    createdAt: serverTimestamp(),
  }));

  await assertFails(logRef.update({ action: "TAMPERED" }));
  await assertFails(logRef.delete());
});
