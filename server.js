require('dotenv').config();
const fs = require('fs');
const path = require('path');
const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const crypto = require('crypto');
const {pool, initializeDatabase, transaction} = require('./server/db');
const chatRouter = require('./server/chat');
const multer = require('multer');

// File upload config
const uploadsDir = path.join(__dirname, 'uploads');
if (!fs.existsSync(uploadsDir)) fs.mkdirSync(uploadsDir, {recursive: true});
const storage = multer.diskStorage({
  destination: (req, file, cb) => cb(null, uploadsDir),
  filename: (req, file, cb) => {
    const ext = path.extname(file.originalname) || '.jpg';
    cb(null, `job-photo-${Date.now()}-${Math.random().toString(36).slice(2, 8)}${ext}`);
  }
});
const upload = multer({
  storage, limits: {fileSize: 5 * 1024 * 1024},
  fileFilter: (req, file, cb) => {
    if (/^image\/(jpeg|jpg|png|gif|webp)$/.test(file.mimetype)) cb(null, true);
    else cb(new Error('Only image files (JPG, PNG, GIF, WebP) are allowed.'));
  }
});

const app = express();
const PORT = Number(process.env.PORT || 8080);
const SECRET = process.env.JWT_SECRET || 'development-secret-change-me';
const staticDir = path.join(__dirname, 'src', 'main', 'resources', 'static');
const otpChallenges = new Map();

app.use(express.json({limit: '1mb'}));
app.use(express.urlencoded({extended: true}));
app.use((req, res, next) => { res.set('Cache-Control', 'no-store'); next(); });

const ok = (res, data, message) => res.json({data, ...(message ? {message} : {})});
const fail = (res, status, message) => res.status(status).json({message});
const guard = (...roles) => (req, res, next) => req.user && roles.includes(req.user.role) ? next() : fail(res, 403, 'You do not have permission for this action');

// Wire up auth before DELETE on reports
app.use('/api/reports/:id', (req, res, next) => req.method === 'DELETE' ? auth(req, res, () => guard('ROLE_STUDENT')(req, res, next)) : next());
app.use('/api/chat', auth, chatRouter);

async function auth(req, res, next) {
  try {
    const token = (req.headers.authorization || '').replace(/^Bearer\s+/i, '');
    if (!token) return fail(res, 401, 'Authentication is required');
    const c = jwt.verify(token, SECRET);
    const [r] = await pool.query('SELECT * FROM users WHERE id=?', [c.id]);
    if (!r[0] || !r[0].is_active || r[0].is_suspended) return fail(res, 401, 'Account is inactive or suspended');
    req.user = r[0];
    next();
  } catch { fail(res, 401, 'Invalid or expired token'); }
}

function token(user, extra = {}) {
  return {
    token: jwt.sign({id: user.id, role: user.role}, SECRET, {expiresIn: '7d'}),
    type: 'Bearer', id: user.id, email: user.email, fullName: user.full_name,
    phone: user.phone, role: user.role, active: !!user.is_active,
    suspended: !!user.is_suspended, ...extra
  };
}

async function sendEmailOtp(destination, code) {
  if (!process.env.SMTP_HOST || !process.env.SMTP_USER || !process.env.SMTP_PASSWORD || !process.env.SMTP_FROM)
    throw Object.assign(new Error('Email OTP is not configured. Add SMTP settings to .env.'), {status: 503});
  const nodemailer = require('nodemailer');
  const transporter = nodemailer.createTransport({
    host: process.env.SMTP_HOST,
    port: Number(process.env.SMTP_PORT || 587),
    secure: process.env.SMTP_SECURE === 'true',
    auth: {user: process.env.SMTP_USER, pass: process.env.SMTP_PASSWORD}
  });
  await transporter.sendMail({
    from: process.env.SMTP_FROM,
    to: destination,
    subject: 'Your PartTime Job verification code',
    text: `Your verification code is ${code}. It expires in 10 minutes.`
  });
}

// ─── AUTH ────────────────────────────────────────────────────────────────────

app.post('/api/auth/request-otp', async (req, res, next) => {
  try {
    const destination = String(req.body.email || '').trim().toLowerCase();
    if (!destination || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(destination)) return fail(res, 400, 'Enter a valid email address');
    const [existing] = await pool.query('SELECT id FROM users WHERE email=?', [destination]);
    if (existing[0]) return fail(res, 409, 'That email is already registered');
    const id = crypto.randomUUID();
    const code = String(crypto.randomInt(100000, 1000000));
    await sendEmailOtp(destination, code);
    otpChallenges.set(id, {destination, code, expiresAt: Date.now() + 10 * 60 * 1000, attempts: 0});
    ok(res, {verificationId: id, expiresInSeconds: 600}, 'Verification code sent by email');
  } catch (e) { next(e); }
});

app.post('/api/auth/verify-otp', async (req, res, next) => {
  try {
    const challenge = otpChallenges.get(req.body.verificationId);
    const otp = String(req.body.otp || '');
    if (!challenge || challenge.expiresAt < Date.now()) return fail(res, 400, 'Request a new verification code');
    if (!/^\d{6}$/.test(otp)) return fail(res, 400, 'OTP must contain exactly 6 digits');
    if (challenge.attempts >= 5) return fail(res, 429, 'Too many incorrect codes. Request a new code');
    if (otp !== challenge.code) { challenge.attempts++; return fail(res, 400, 'Incorrect verification code'); }
    challenge.verified = true;
    ok(res, null, 'Email verified successfully');
  } catch (e) { next(e); }
});

app.post('/api/auth/register', async (req, res, next) => {
  try {
    const b = req.body;
    if (!b.fullName || !b.email || !b.password || !b.phone || !['ROLE_STUDENT', 'ROLE_OWNER'].includes(b.role))
      return fail(res, 400, 'Full name, email, password, phone and a valid role are required');
    const challenge = otpChallenges.get(b.verificationId);
    const destination = String(b.email).trim().toLowerCase();
    if (!challenge || !challenge.verified || challenge.destination !== destination || challenge.expiresAt < Date.now())
      return fail(res, 400, 'Verify your email before creating the account');
    otpChallenges.delete(b.verificationId);
    const data = await transaction(async c => {
      const [x] = await c.query('INSERT INTO users (email,password_hash,full_name,phone,role) VALUES (?,?,?,?,?)',
        [b.email.toLowerCase(), await bcrypt.hash(b.password, 10), b.fullName, b.phone, b.role]);
      if (b.role === 'ROLE_STUDENT') {
        await c.query('INSERT INTO student_profiles (user_id,college_name,preferred_area,skills,bio,emergency_contact) VALUES (?,?,?,?,?,?)',
          [x.insertId, b.collegeName || null, b.preferredArea || null, b.skills || null, b.bio || null, b.emergencyContact || null]);
      } else {
        await c.query('INSERT INTO owner_profiles (user_id,catering_name,business_address,business_phone) VALUES (?,?,?,?)',
          [x.insertId, b.cateringName || b.fullName, b.businessAddress || null, b.businessPhone || b.phone]);
      }
      const [u] = await c.query('SELECT * FROM users WHERE id=?', [x.insertId]);
      return token(u[0]);
    });
    ok(res, data, 'Registration successful! Welcome to PartTime Job.');
  } catch (e) {
    if (e.code === 'ER_DUP_ENTRY') return fail(res, 409, 'Email or phone is already registered');
    next(e);
  }
});

app.post('/api/auth/login', async (req, res, next) => {
  try {
    const [r] = await pool.query('SELECT * FROM users WHERE email=?', [(req.body.email || '').toLowerCase()]);
    const u = r[0];
    if (!u || !(await bcrypt.compare(req.body.password || '', u.password_hash))) return fail(res, 401, 'Invalid email or password');
    if (u.is_suspended || !u.is_active) return fail(res, 403, 'Your account is suspended or inactive');
    let extra = {};
    if (u.role === 'ROLE_STUDENT') {
      const [p] = await pool.query('SELECT preferred_area FROM student_profiles WHERE user_id=?', [u.id]);
      extra.preferredArea = p[0]?.preferred_area;
    }
    if (u.role === 'ROLE_OWNER') {
      const [p] = await pool.query('SELECT id,catering_name,verification_status FROM owner_profiles WHERE user_id=?', [u.id]);
      extra = {profileId: p[0]?.id, cateringName: p[0]?.catering_name, verificationStatus: p[0]?.verification_status};
    }
    ok(res, token(u, extra), 'Login successful!');
  } catch (e) { next(e); }
});

// ─── JOBS ────────────────────────────────────────────────────────────────────

const jobSql = `SELECT j.*,o.catering_name,o.verification_status,u.id owner_user_id,u.full_name owner_name FROM catering_jobs j JOIN owner_profiles o ON o.id=j.owner_id JOIN users u ON u.id=o.user_id`;
const appSql = `SELECT a.*,j.title job_title,j.work_type,j.work_area,j.detailed_location,j.job_date,j.start_time,j.end_time,j.payment_type,j.is_on_spot_payment,j.contact_phone,j.contact_email,o.catering_name,o.verification_status,ou.id owner_user_id,ou.full_name owner_name,su.id student_user_id,su.full_name student_name,su.email student_email,su.phone student_phone,sp.college_name,sp.skills,sp.rating student_rating,sp.total_jobs_completed FROM job_applications a JOIN catering_jobs j ON j.id=a.job_id JOIN owner_profiles o ON o.id=j.owner_id JOIN users ou ON ou.id=o.user_id JOIN student_profiles sp ON sp.id=a.student_id JOIN users su ON su.id=sp.user_id`;

function job(r, unlocked = false) {
  return {
    id: r.id, title: r.title, description: r.description,
    workType: r.work_type, workTypeDisplayName: r.work_type,
    workArea: r.work_area, detailedLocation: unlocked ? r.detailed_location : undefined,
    jobDate: r.job_date, startTime: r.start_time, endTime: r.end_time,
    paymentAmount: r.payment_amount, paymentType: r.payment_type,
    paymentTypeDisplayName: r.payment_type, onSpotPayment: !!r.is_on_spot_payment,
    workersRequired: r.workers_required, workersSelected: r.workers_selected,
    requiredSkills: r.required_skills,
    contactPhone: unlocked ? r.contact_phone : undefined,
    contactEmail: unlocked ? r.contact_email : undefined,
    locationPhotoUrl: r.location_photo_url || null,
    applyDeadline: r.apply_deadline || null,
    status: r.status, cateringName: r.catering_name,
    ownerId: r.owner_user_id,
    ownerVerified: r.verification_status === 'VERIFIED',
    createdAt: r.created_at
  };
}

function application(r, unlocked = false) {
  return {
    id: r.id, jobId: r.job_id, jobTitle: r.job_title,
    workType: r.work_type, workTypeDisplayName: r.work_type,
    workArea: r.work_area,
    detailedLocation: unlocked ? r.detailed_location : undefined,
    locationUnlocked: unlocked,
    jobDate: r.job_date, startTime: r.start_time, endTime: r.end_time,
    paymentType: r.payment_type, paymentTypeDisplayName: r.payment_type,
    onSpotPayment: !!r.is_on_spot_payment,
    contactPhone: unlocked ? r.contact_phone : undefined,
    contactEmail: unlocked ? r.contact_email : undefined,
    contactUnlocked: unlocked,
    ownerId: r.owner_user_id, cateringName: r.catering_name,
    ownerName: r.owner_name,
    ownerVerified: r.verification_status === 'VERIFIED',
    studentId: r.student_id, studentUserId: r.student_user_id,
    studentName: r.student_name, studentEmail: r.student_email,
    studentPhone: r.student_phone, collegeName: r.college_name,
    skills: r.skills, studentRating: r.student_rating,
    totalJobsCompleted: r.total_jobs_completed,
    status: r.status, attendanceStatus: r.attendance_status,
    workCompletionStatus: r.work_completion_status,
    paymentStatus: r.payment_status, paymentAmount: r.payment_amount,
    paymentConfirmationDate: r.payment_confirmation_date,
    notes: r.notes, appliedAt: r.applied_at,
    respondedAt: r.responded_at, createdAt: r.created_at
  };
}

function profile(r) {
  return r ? {
    id: r.id, userId: r.user_id, fullName: r.full_name,
    email: r.email, phone: r.phone,
    collegeName: r.college_name, preferredArea: r.preferred_area,
    skills: r.skills, bio: r.bio,
    emergencyContact: r.emergency_contact,
    profilePhotoUrl: r.profile_photo_url,
    totalJobsCompleted: r.total_jobs_completed, rating: r.rating,
    cateringName: r.catering_name,
    businessAddress: r.business_address,
    businessPhone: r.business_phone,
    verified: r.verification_status === 'VERIFIED',
    verificationStatus: r.verification_status,
    verifiedAt: r.verified_at,
    totalJobsPosted: r.total_jobs_posted,
    profilePhotoUrl: r.profile_photo_url
  } : null;
}

async function listJobs(req, res, next) {
  try {
    let sql = `${jobSql} WHERE j.status='OPEN' AND u.is_suspended=FALSE AND (j.apply_deadline IS NULL OR j.apply_deadline > NOW())`, v = [];
    const q = req.query;
    if (q.area || q.location) { sql += ' AND LOWER(j.work_area) LIKE LOWER(?)'; v.push(`%${q.area || q.location}%`); }
    if (q.workType) { sql += ' AND j.work_type=?'; v.push(q.workType); }
    if (q.jobDate) { sql += ' AND j.job_date=?'; v.push(q.jobDate); }
    if (q.minPayment) { sql += ' AND j.payment_amount>=?'; v.push(q.minPayment); }
    if (q.maxPayment) { sql += ' AND j.payment_amount<=?'; v.push(q.maxPayment); }
    if (q.paymentType) { sql += ' AND j.payment_type=?'; v.push(q.paymentType); }
    sql += ' ORDER BY j.job_date ASC,j.created_at DESC';
    const [r] = await pool.query(sql, v);
    ok(res, r.map(x => job(x)), 'Jobs retrieved successfully');
  } catch (e) { next(e); }
}

app.get('/api/public/jobs', listJobs);
app.get('/api/public/jobs/recommended', listJobs);

app.get('/api/public/jobs/:id', async (req, res, next) => {
  try {
    const [r] = await pool.query(`${jobSql} WHERE j.id=?`, [req.params.id]);
    if (!r[0]) return fail(res, 404, 'Job not found');
    const j = job(r[0]);
    j.ownerName = r[0].owner_name;
    ok(res, j, 'Job details retrieved');
  } catch (e) { next(e); }
});

// ─── STUDENT ─────────────────────────────────────────────────────────────────

async function current(req, kind) {
  const table = kind === 'student' ? 'student_profiles' : 'owner_profiles';
  const [r] = await pool.query(`SELECT u.*,p.* FROM users u JOIN ${table} p ON p.user_id=u.id WHERE u.id=?`, [req.user.id]);
  return r[0];
}

app.get('/api/student/profile', auth, guard('ROLE_STUDENT'), async (req, res, next) => {
  try { ok(res, profile(await current(req, 'student'))); } catch (e) { next(e); }
});

app.put('/api/student/profile', auth, guard('ROLE_STUDENT'), async (req, res, next) => {
  try {
    const b = req.body;
    await transaction(async c => {
      await c.query('UPDATE users SET full_name=COALESCE(?,full_name),phone=COALESCE(?,phone) WHERE id=?', [b.fullName, b.phone, req.user.id]);
      await c.query('UPDATE student_profiles SET college_name=?,preferred_area=?,skills=?,bio=?,emergency_contact=?,profile_photo_url=COALESCE(?,profile_photo_url) WHERE user_id=?',
        [b.collegeName || null, b.preferredArea || null, b.skills || null, b.bio || null, b.emergencyContact || null, b.profilePhotoUrl || null, req.user.id]);
    });
    ok(res, profile(await current(req, 'student')), 'Profile updated successfully');
  } catch (e) { next(e); }
});

app.get('/api/student/dashboard', auth, guard('ROLE_STUDENT'), async (req, res, next) => {
  try {
    const [r] = await pool.query(`SELECT
      (SELECT COUNT(*) FROM catering_jobs WHERE status='OPEN') availableJobsCount,
      (SELECT COUNT(*) FROM job_applications a JOIN student_profiles s ON s.id=a.student_id WHERE s.user_id=?) totalApplicationsCount,
      (SELECT COUNT(*) FROM job_applications a JOIN student_profiles s ON s.id=a.student_id WHERE s.user_id=? AND a.status='ACCEPTED') acceptedApplicationsCount,
      (SELECT COUNT(*) FROM job_applications a JOIN student_profiles s ON s.id=a.student_id WHERE s.user_id=? AND a.status='APPLIED') appliedJobsCount,
      (SELECT COUNT(*) FROM job_applications a JOIN student_profiles s ON s.id=a.student_id WHERE s.user_id=? AND a.work_completion_status='COMPLETED') completedJobsCount,
      (SELECT COALESCE(SUM(payment_amount),0) FROM job_applications a JOIN student_profiles s ON s.id=a.student_id WHERE s.user_id=? AND a.payment_status='CONFIRMED') totalEarnings,
      (SELECT COUNT(*) FROM notifications WHERE recipient_id=? AND is_read=FALSE) unreadNotificationsCount`,
      [req.user.id, req.user.id, req.user.id, req.user.id, req.user.id, req.user.id]);
    ok(res, r[0]);
  } catch (e) { next(e); }
});

app.get('/api/student/jobs', auth, guard('ROLE_STUDENT'), listJobs);

app.post('/api/student/jobs/:jobId/apply', auth, guard('ROLE_STUDENT'), async (req, res, next) => {
  try {
    const data = await transaction(async c => {
      const [[s]] = await c.query('SELECT id FROM student_profiles WHERE user_id=?', [req.user.id]);
      const [[j]] = await c.query('SELECT * FROM catering_jobs WHERE id=? FOR UPDATE', [req.params.jobId]);
      if (!s || !j || j.status !== 'OPEN') throw Object.assign(new Error('Job is not available'), {status: 400});
      // Check application deadline
      if (j.apply_deadline) {
        const deadline = new Date(j.apply_deadline);
        if (new Date() > deadline) throw Object.assign(new Error('The application deadline for this job has passed.'), {status: 400});
      }
      const [x] = await c.query('INSERT INTO job_applications (job_id,student_id,payment_amount,notes) VALUES (?,?,?,?)',
        [j.id, s.id, j.payment_amount, req.body.notes || null]);
      const [r] = await c.query(`${appSql} WHERE a.id=?`, [x.insertId]);
      return application(r[0]);
    });
    ok(res, data, 'Application submitted successfully!');
  } catch (e) {
    if (e.code === 'ER_DUP_ENTRY') return fail(res, 409, 'You have already applied for this job');
    next(e);
  }
});

async function studentApps(req, res, next, filter = '') {
  try {
    const [r] = await pool.query(`${appSql} WHERE su.id=? ${filter} ORDER BY a.created_at DESC`, [req.user.id]);
    ok(res, r.map(x => application(x, x.status === 'ACCEPTED' || x.work_completion_status === 'COMPLETED')));
  } catch (e) { next(e); }
}

app.get('/api/student/applications', auth, guard('ROLE_STUDENT'), studentApps);
app.get('/api/student/applications/accepted', auth, guard('ROLE_STUDENT'), (q, s, n) => studentApps(q, s, n, "AND a.status='ACCEPTED'"));
app.get('/api/student/applications/completed', auth, guard('ROLE_STUDENT'), (q, s, n) => studentApps(q, s, n, "AND a.work_completion_status='COMPLETED'"));

app.delete('/api/student/applications/:id', auth, guard('ROLE_STUDENT'), async (req, res, next) => {
  try {
    const [r] = await pool.query("UPDATE job_applications a JOIN student_profiles s ON s.id=a.student_id SET a.status='CANCELLED' WHERE a.id=? AND s.user_id=? AND a.status='APPLIED'",
      [req.params.id, req.user.id]);
    if (!r.affectedRows) return fail(res, 400, 'Only pending applications can be cancelled');
    ok(res, null, 'Application cancelled successfully');
  } catch (e) { next(e); }
});

app.put('/api/student/applications/:id/confirm-payment', auth, guard('ROLE_STUDENT'), async (req, res, next) => {
  try {
    await transaction(async c => {
      const [r] = await c.query("UPDATE payment_records p JOIN student_profiles s ON s.id=p.student_id SET p.payment_status='CONFIRMED',p.confirmed_paid_at=NOW(),p.notes=? WHERE p.application_id=? AND s.user_id=?",
        [req.body.notes || null, req.params.id, req.user.id]);
      if (!r.affectedRows) throw Object.assign(new Error('Payment record not found'), {status: 404});
      await c.query("UPDATE job_applications SET payment_status='CONFIRMED',payment_confirmation_date=NOW() WHERE id=?", [req.params.id]);
    });
    ok(res, null, 'Payment confirmed successfully!');
  } catch (e) { next(e); }
});

app.get('/api/student/payments', auth, guard('ROLE_STUDENT'), async (req, res, next) => {
  try {
    const [r] = await pool.query('SELECT p.*,j.title job_title,j.work_area,o.catering_name owner_catering_name FROM payment_records p JOIN catering_jobs j ON j.id=p.job_id JOIN student_profiles s ON s.id=p.student_id JOIN owner_profiles o ON o.id=p.owner_id WHERE s.user_id=? ORDER BY p.created_at DESC', [req.user.id]);
    ok(res, r.map(x => ({
      id: x.id, applicationId: x.application_id, jobId: x.job_id,
      studentId: x.student_id, ownerId: x.owner_id,
      amount: x.amount, paymentType: x.payment_type,
      paymentTypeDisplayName: x.payment_type,
      paymentStatus: x.payment_status,
      markedPaidAt: x.marked_paid_at, confirmedPaidAt: x.confirmed_paid_at,
      notes: x.notes, createdAt: x.created_at,
      jobTitle: x.job_title, workArea: x.work_area,
      ownerCateringName: x.owner_catering_name
    })));
  } catch (e) { next(e); }
});

// ─── OWNER ───────────────────────────────────────────────────────────────────

async function ownerId(req) {
  const [r] = await pool.query('SELECT id FROM owner_profiles WHERE user_id=?', [req.user.id]);
  return r[0]?.id;
}

app.get('/api/owner/profile', auth, guard('ROLE_OWNER'), async (req, res, next) => {
  try { ok(res, profile(await current(req, 'owner'))); } catch (e) { next(e); }
});

app.put('/api/owner/profile', auth, guard('ROLE_OWNER'), async (req, res, next) => {
  try {
    const b = req.body;
    await transaction(async c => {
      await c.query('UPDATE users SET full_name=COALESCE(?,full_name),phone=COALESCE(?,phone) WHERE id=?', [b.fullName, b.phone, req.user.id]);
      await c.query('UPDATE owner_profiles SET catering_name=COALESCE(?,catering_name),business_address=?,business_phone=?,profile_photo_url=COALESCE(?,profile_photo_url) WHERE user_id=?',
        [b.cateringName, b.businessAddress || null, b.businessPhone || null, b.profilePhotoUrl || null, req.user.id]);
    });
    ok(res, profile(await current(req, 'owner')), 'Profile updated successfully');
  } catch (e) { next(e); }
});

app.post('/api/owner/jobs', auth, guard('ROLE_OWNER'), async (req, res, next) => {
  try {
    const b = req.body, id = await ownerId(req);
    const [x] = await pool.query(`INSERT INTO catering_jobs (owner_id,title,description,work_type,work_area,detailed_location,job_date,start_time,end_time,payment_amount,payment_type,is_on_spot_payment,workers_required,required_skills,contact_phone,contact_email,location_photo_url,apply_deadline) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)`,
      [id, b.title, b.description || null, b.workType, b.workArea, b.detailedLocation, b.jobDate, b.startTime, b.endTime, b.paymentAmount, b.paymentType, b.onSpotPayment !== false, b.workersRequired, b.requiredSkills || null, b.contactPhone, b.contactEmail || null, b.locationPhotoUrl || null, b.applyDeadline || null]);
    const [r] = await pool.query(`${jobSql} WHERE j.id=?`, [x.insertId]);
    ok(res, job(r[0], true), 'Job posted successfully!');
  } catch (e) { next(e); }
});

app.get('/api/owner/jobs', auth, guard('ROLE_OWNER'), async (req, res, next) => {
  try {
    const oid = await ownerId(req);
    const [r] = await pool.query(`${jobSql} WHERE j.owner_id=? ORDER BY j.created_at DESC`, [oid]);
    const jobIds = r.map(x => x.id);
    let acceptedMap = {};
    let cancellationMap = {};
    if (jobIds.length > 0) {
      const placeholders = jobIds.map(() => '?').join(',');
      const [acceptedRows] = await pool.query(
        `SELECT job_id, COUNT(*) cnt FROM job_applications WHERE job_id IN (${placeholders}) AND status='ACCEPTED' GROUP BY job_id`,
        jobIds
      );
      acceptedRows.forEach(row => { acceptedMap[row.job_id] = row.cnt > 0; });

      const [delRows] = await pool.query(
        `SELECT job_id, COUNT(*) cnt FROM job_deletion_requests WHERE job_id IN (${placeholders}) AND status='PENDING' GROUP BY job_id`,
        jobIds
      );
      delRows.forEach(row => { cancellationMap[row.job_id] = row.cnt > 0; });
    }
    // Compute canDelete per job based on 3 scenarios
    const now = new Date();
    ok(res, r.map(x => {
      const hasAccepted = !!acceptedMap[x.id];
      const hasPendingCancel = !!cancellationMap[x.id];
      const jobEnd = new Date(`${x.job_date}T${x.end_time || '23:59:59'}`);
      const timeCrossed = now > jobEnd;

      const deadlinePassed = !!(x.apply_deadline && new Date(x.apply_deadline) <= now);
      // canDelete = (no one hired) OR (time crossed) OR (deadline passed) OR (cancellation requested)
      const canDelete = !hasAccepted || timeCrossed || deadlinePassed || hasPendingCancel;
      return {...job(x, true), canDelete, hasPendingDeletionRequest: hasPendingCancel};
    }));
  } catch (e) { next(e); }
});

app.get('/api/owner/jobs/closed', auth, guard('ROLE_OWNER'), async (req, res, next) => {
  try {
    const oid = await ownerId(req);
    // Closed jobs = jobs where apply_deadline has passed OR status is CANCELLED/COMPLETED/FILLED
    const [r] = await pool.query(
      `${jobSql} WHERE j.owner_id=? AND (j.status IN ('CANCELLED','COMPLETED') OR (j.apply_deadline IS NOT NULL AND j.apply_deadline <= NOW())) ORDER BY j.updated_at DESC`,
      [oid]
    );
    if (r.length === 0) return ok(res, []);

    const jobIds = r.map(x => x.id);
    const placeholders = jobIds.map(() => '?').join(',');

    // Get all applicants for these jobs with their details
    const [apps] = await pool.query(
      `${appSql} WHERE j.id IN (${placeholders}) ORDER BY j.id, a.applied_at ASC`,
      jobIds
    );

    // Group applications by job
    const appsByJob = {};
    apps.forEach(a => {
      if (!appsByJob[a.job_id]) appsByJob[a.job_id] = [];
      appsByJob[a.job_id].push(application(a, true));
    });

    ok(res, r.map(x => {
      const j = job(x, true);
      j.applicants = appsByJob[x.id] || [];
      j.isDeadlinePassed = !!(x.apply_deadline && new Date(x.apply_deadline) <= new Date());
      return j;
    }));
  } catch (e) { next(e); }
});

app.get('/api/owner/jobs/:id', auth, guard('ROLE_OWNER'), async (req, res, next) => {
  try {
    const [r] = await pool.query(`${jobSql} WHERE j.id=? AND j.owner_id=?`, [req.params.id, await ownerId(req)]);
    if (!r[0]) return fail(res, 404, 'Job not found');
    const [a] = await pool.query(`${appSql} WHERE j.id=?`, [req.params.id]);
    ok(res, {...job(r[0], true), applications: a.map(x => application(x, true))});
  } catch (e) { next(e); }
});

app.put('/api/owner/jobs/:id', auth, guard('ROLE_OWNER'), async (req, res, next) => {
  try {
    const b = req.body;
    await pool.query('UPDATE catering_jobs SET title=?,description=?,work_type=?,work_area=?,detailed_location=?,job_date=?,start_time=?,end_time=?,payment_amount=?,payment_type=?,is_on_spot_payment=?,workers_required=?,required_skills=?,contact_phone=?,contact_email=?,location_photo_url=?,apply_deadline=? WHERE id=? AND owner_id=?',
      [b.title, b.description, b.workType, b.workArea, b.detailedLocation, b.jobDate, b.startTime, b.endTime, b.paymentAmount, b.paymentType, b.onSpotPayment, b.workersRequired, b.requiredSkills, b.contactPhone, b.contactEmail, b.locationPhotoUrl || null, b.applyDeadline || null, req.params.id, await ownerId(req)]);
    const [r] = await pool.query(`${jobSql} WHERE j.id=?`, [req.params.id]);
    ok(res, job(r[0], true), 'Job updated successfully');
  } catch (e) { next(e); }
});

app.delete('/api/owner/jobs/:id', auth, guard('ROLE_OWNER'), async (req, res, next) => {
  try {
    const oid = await ownerId(req);
    const data = await transaction(async c => {
      // 1. Verify job exists and belongs to this owner
      const [[jobRow]] = await c.query('SELECT id, status, job_date, end_time FROM catering_jobs WHERE id=? AND owner_id=? FOR UPDATE', [req.params.id, oid]);
      if (!jobRow) throw Object.assign(new Error('Job not found'), {status: 404});

      // 2. Find all hired (ACCEPTED) students
      const [acceptedApps] = await c.query(
        `SELECT a.id, a.student_id, su.full_name student_name
         FROM job_applications a
         JOIN student_profiles sp ON sp.id=a.student_id
         JOIN users su ON su.id=sp.user_id
         WHERE a.job_id=? AND a.status='ACCEPTED'`,
        [jobRow.id]
      );

      if (acceptedApps.length === 0) {
        // CASE A: No student hired → delete immediately
        await c.query('DELETE FROM catering_jobs WHERE id=? AND owner_id=?', [jobRow.id, oid]);
        return {deleted: true};
      }

      // CASE B: Student(s) hired → check for existing pending request
      const [[existingPending]] = await c.query(
        "SELECT COUNT(*) cnt FROM job_deletion_requests WHERE job_id=? AND status='PENDING'",
        [jobRow.id]
      );
      if (existingPending.cnt > 0) {
        throw Object.assign(new Error('A deletion request is already waiting for the hired student(s) to respond.'), {status: 400});
      }

      // Create deletion request for EACH hired student
      for (const app of acceptedApps) {
        await c.query(
          'INSERT INTO job_deletion_requests (job_id, owner_id, student_id) VALUES (?,?,?)',
          [jobRow.id, oid, app.student_id]
        );
        // Notify the hired student
        await c.query(
          "INSERT INTO notifications (recipient_id,title,message,type,related_entity_id) VALUES (?,?,?,?,?)",
          [app.student_id, 'Job Deletion Request',
           `The owner wants to delete the job you were hired for. Do you want to approve this request?`,
           'JOB_DELETION_REQUEST', null]
        );
      }
      return {deleted: false, requestCreated: true, hiredStudentCount: acceptedApps.length};
    });
    if (data.deleted) {
      ok(res, data, 'Job deleted successfully');
    } else {
      ok(res, data, 'Deletion request sent to the hired student(s).');
    }
  } catch (e) { next(e); }
});

// ─── STUDENT JOB DELETION REQUESTS ──────────────────────────────────────────

app.get('/api/student/deletion-requests', auth, guard('ROLE_STUDENT'), async (req, res, next) => {
  try {
    const [[sp]] = await pool.query('SELECT id FROM student_profiles WHERE user_id=?', [req.user.id]);
    if (!sp) return ok(res, []);
    const [rows] = await pool.query(
      `SELECT dr.*, j.title job_title, j.work_area, j.job_date, j.start_time, j.end_time,
              o.catering_name, u.full_name owner_name
       FROM job_deletion_requests dr
       JOIN catering_jobs j ON j.id=dr.job_id
       JOIN owner_profiles o ON o.id=dr.owner_id
       JOIN users u ON u.id=o.user_id
       WHERE dr.student_id=?
       ORDER BY dr.created_at DESC`,
      [sp.id]
    );
    ok(res, rows.map(r => ({
      id: r.id, jobId: r.job_id, ownerId: r.owner_id,
      status: r.status, createdAt: r.created_at, respondedAt: r.responded_at,
      jobTitle: r.job_title, workArea: r.work_area,
      jobDate: r.job_date, startTime: r.start_time, endTime: r.end_time,
      cateringName: r.catering_name, ownerName: r.owner_name
    })));
  } catch (e) { next(e); }
});

app.post('/api/student/deletion-requests/:id/accept', auth, guard('ROLE_STUDENT'), async (req, res, next) => {
  try {
    const [[sp]] = await pool.query('SELECT id FROM student_profiles WHERE user_id=?', [req.user.id]);
    if (!sp) return fail(res, 404, 'Student profile not found');
    const data = await transaction(async c => {
      // 1. Verify request exists, belongs to this student, and is PENDING
      const [[reqRow]] = await c.query(
        'SELECT * FROM job_deletion_requests WHERE id=? AND student_id=? AND status=? FOR UPDATE',
        [req.params.id, sp.id, 'PENDING']
      );
      if (!reqRow) throw Object.assign(new Error('This deletion request is no longer available.'), {status: 404});

      // 2. Mark request as ACCEPTED
      await c.query("UPDATE job_deletion_requests SET status='ACCEPTED',responded_at=NOW() WHERE id=?", [reqRow.id]);

      // 3. Delete the job (CASCADE handles applications, payments, other deletion requests)
      await c.query('DELETE FROM catering_jobs WHERE id=?', [reqRow.job_id]);

      // 4. Notify the owner
      await c.query(
        "INSERT INTO notifications (recipient_id,title,message,type,related_entity_id) VALUES (?,?,?,?,?)",
        [reqRow.owner_id, 'Deletion Request Accepted',
         'The hired student accepted your job deletion request. The job has been deleted.',
         'JOB_DELETION_ACCEPTED', reqRow.id]
      );
    });
    ok(res, data, 'Request accepted. The job has been deleted.');
  } catch (e) { next(e); }
});

app.post('/api/student/deletion-requests/:id/reject', auth, guard('ROLE_STUDENT'), async (req, res, next) => {
  try {
    const [[sp]] = await pool.query('SELECT id FROM student_profiles WHERE user_id=?', [req.user.id]);
    if (!sp) return fail(res, 404, 'Student profile not found');
    const data = await transaction(async c => {
      // 1. Verify request exists, belongs to this student, and is PENDING
      const [[reqRow]] = await c.query(
        'SELECT * FROM job_deletion_requests WHERE id=? AND student_id=? AND status=? FOR UPDATE',
        [req.params.id, sp.id, 'PENDING']
      );
      if (!reqRow) throw Object.assign(new Error('This deletion request is no longer available.'), {status: 404});

      // 2. Mark as REJECTED
      await c.query("UPDATE job_deletion_requests SET status='REJECTED',responded_at=NOW() WHERE id=?", [reqRow.id]);

      // 3. Notify the owner
      await c.query(
        "INSERT INTO notifications (recipient_id,title,message,type,related_entity_id) VALUES (?,?,?,?,?)",
        [reqRow.owner_id, 'Deletion Request Rejected',
         'The hired student rejected your job deletion request. The job remains active.',
         'JOB_DELETION_REJECTED', reqRow.id]
      );
    });
    ok(res, data, 'Request rejected. The job remains active.');
  } catch (e) { next(e); }
});

app.get('/api/owner/jobs/:id/applications', auth, guard('ROLE_OWNER'), async (req, res, next) => {
  try {
    const [r] = await pool.query(`${appSql} WHERE j.id=? AND j.owner_id=?`, [req.params.id, await ownerId(req)]);
    ok(res, r.map(x => application(x, true)));
  } catch (e) { next(e); }
});

async function appAction(req, res, next, accept) {
  try {
    const id = await ownerId(req);
    const data = await transaction(async c => {
      const [r] = await c.query(`${appSql} WHERE a.id=? AND j.owner_id=? FOR UPDATE`, [req.params.id, id]);
      const a = r[0];
      if (!a) throw Object.assign(new Error('Application not found'), {status: 404});
      if (accept) {
        const [[count]] = await c.query("SELECT COUNT(*) count FROM job_applications WHERE job_id=? AND status='ACCEPTED'", [a.job_id]);
        const [[j]] = await c.query('SELECT workers_required FROM catering_jobs WHERE id=?', [a.job_id]);
        if (count.count >= j.workers_required) throw Object.assign(new Error('Worker limit has been reached'), {status: 400});
        await c.query("UPDATE job_applications SET status='ACCEPTED',responded_at=NOW() WHERE id=?", [a.id]);
        await c.query("UPDATE catering_jobs SET workers_selected=workers_selected+1,status=IF(workers_selected+1>=workers_required,'FILLED','OPEN') WHERE id=?", [a.job_id]);
        await c.query('INSERT INTO payment_records (application_id,job_id,student_id,owner_id,amount,payment_type) SELECT a.id,a.job_id,a.student_id,?,a.payment_amount,j.payment_type FROM job_applications a JOIN catering_jobs j ON j.id=a.job_id WHERE a.id=?', [id, a.id]);
        await c.query("INSERT INTO notifications (recipient_id,title,message,type,related_entity_id) VALUES (?,?,?,?,?)",
          [a.student_user_id, 'Application Accepted', `Your application for "${a.job_title}" was accepted. Check Accepted Shifts for venue and contact details.`, 'APPLICATION_ACCEPTED', a.id]);
      } else {
        await c.query("UPDATE job_applications SET status='REJECTED',responded_at=NOW() WHERE id=?", [a.id]);
        await c.query("INSERT INTO notifications (recipient_id,title,message,type,related_entity_id) VALUES (?,?,?,?,?)",
          [a.student_user_id, 'Application Update', `Your application for "${a.job_title}" was not accepted.`, 'APPLICATION_REJECTED', a.id]);
      }
      const [x] = await c.query(`${appSql} WHERE a.id=?`, [a.id]);
      return application(x[0], true);
    });
    ok(res, data, accept ? 'Applicant accepted successfully!' : 'Applicant rejected.');
  } catch (e) { next(e); }
}

app.put('/api/owner/applications/:id/accept', auth, guard('ROLE_OWNER'), (q, s, n) => appAction(q, s, n, true));
app.put('/api/owner/applications/:id/reject', auth, guard('ROLE_OWNER'), (q, s, n) => appAction(q, s, n, false));

app.put('/api/owner/applications/:id/attendance', auth, guard('ROLE_OWNER'), async (req, res, next) => {
  try {
    await pool.query('UPDATE job_applications a JOIN catering_jobs j ON j.id=a.job_id SET a.attendance_status=?,a.work_completion_status=? WHERE a.id=? AND j.owner_id=?',
      [req.body.attendanceStatus, req.body.workCompletionStatus || 'COMPLETED', req.params.id, await ownerId(req)]);
    ok(res, null, 'Attendance updated successfully');
  } catch (e) { next(e); }
});

app.put('/api/owner/jobs/:id/complete', auth, guard('ROLE_OWNER'), async (req, res, next) => {
  try {
    const id = await ownerId(req);
    await transaction(async c => {
      await c.query("UPDATE catering_jobs SET status='COMPLETED' WHERE id=? AND owner_id=?", [req.params.id, id]);
      await c.query("UPDATE job_applications SET work_completion_status='COMPLETED' WHERE job_id=? AND status='ACCEPTED'", [req.params.id]);
    });
    ok(res, null, 'Job marked as completed!');
  } catch (e) { next(e); }
});

app.put('/api/owner/applications/:id/payment', auth, guard('ROLE_OWNER'), async (req, res, next) => {
  try {
    await transaction(async c => {
      await c.query("UPDATE payment_records SET payment_status='PAID',marked_paid_at=NOW(),notes=? WHERE application_id=? AND owner_id=?",
        [req.body.notes || null, req.params.id, await ownerId(req)]);
      await c.query("UPDATE job_applications SET payment_status='PAID' WHERE id=?", [req.params.id]);
    });
    ok(res, null, 'Payment marked as PAID successfully!');
  } catch (e) { next(e); }
});

app.get('/api/owner/payments', auth, guard('ROLE_OWNER'), async (req, res, next) => {
  try {
    const [r] = await pool.query('SELECT p.*,j.title job_title,j.work_area,su.full_name student_name,su.email student_email FROM payment_records p JOIN catering_jobs j ON j.id=p.job_id JOIN student_profiles s ON s.id=p.student_id JOIN users su ON su.id=s.user_id WHERE p.owner_id=? ORDER BY p.created_at DESC', [await ownerId(req)]);
    ok(res, r.map(x => ({
      id: x.id, applicationId: x.application_id, jobId: x.job_id,
      studentId: x.student_id, ownerId: x.owner_id,
      amount: x.amount, paymentType: x.payment_type,
      paymentTypeDisplayName: x.payment_type,
      paymentStatus: x.payment_status,
      markedPaidAt: x.marked_paid_at, confirmedPaidAt: x.confirmed_paid_at,
      notes: x.notes, createdAt: x.created_at,
      jobTitle: x.job_title, workArea: x.work_area,
      studentName: x.student_name, studentEmail: x.student_email
    })));
  } catch (e) { next(e); }
});

app.get('/api/owner/complaints', auth, guard('ROLE_OWNER'), async (req, res, next) => {
  try {
    const [r] = await pool.query(`SELECT r.*,j.title job_title,j.work_area,j.job_date,
      su.full_name student_name,su.email student_email,su.phone student_phone,
      sp.skills student_skills,sp.preferred_area student_area,
      a.status application_status,a.payment_status application_payment_status,a.payment_amount assigned_amount
      FROM reports r JOIN users su ON su.id=r.reporter_id
      LEFT JOIN student_profiles sp ON sp.user_id=su.id
      LEFT JOIN catering_jobs j ON j.id=r.job_id
      LEFT JOIN job_applications a ON a.id=r.application_id
      WHERE r.target_user_id=? ORDER BY r.created_at DESC`, [req.user.id]);
    ok(res, r.map(x => ({
      ...x,
      studentName: x.student_name, studentEmail: x.student_email,
      studentPhone: x.student_phone, studentSkills: x.student_skills,
      studentArea: x.student_area, jobTitle: x.job_title,
      workArea: x.work_area, jobDate: x.job_date,
      applicationStatus: x.application_status,
      applicationPaymentStatus: x.application_payment_status,
      assignedAmount: x.assigned_amount, reportType: x.report_type,
      expectedAmount: x.expected_amount, receivedAmount: x.received_amount,
      evidenceNotes: x.evidence_notes, adminRemarks: x.admin_remarks,
      createdAt: x.created_at
    })));
  } catch (e) { next(e); }
});

// ─── ADMIN ───────────────────────────────────────────────────────────────────

app.get('/api/admin/dashboard', auth, guard('ROLE_ADMIN'), async (req, res, next) => {
  try {
    const [r] = await pool.query(`SELECT
      (SELECT COUNT(*) FROM student_profiles) totalStudentsCount,
      (SELECT COUNT(*) FROM owner_profiles) totalOwnersCount,
      (SELECT COUNT(*) FROM owner_profiles WHERE verification_status='VERIFIED') verifiedOwnersCount,
      (SELECT COUNT(*) FROM catering_jobs WHERE status IN ('OPEN','FILLED')) totalActiveJobsCount,
      (SELECT COUNT(*) FROM catering_jobs WHERE status='COMPLETED') totalCompletedJobsCount,
      (SELECT COUNT(*) FROM job_applications) totalApplicationsCount,
      (SELECT COUNT(*) FROM reports WHERE status='PENDING') pendingDisputesCount,
      (SELECT COUNT(*) FROM users WHERE is_suspended=TRUE) suspendedUsersCount,
      (SELECT COALESCE(SUM(amount),0) FROM payment_records WHERE payment_status IN ('PAID','CONFIRMED')) totalPlatformPayout`);
    ok(res, r[0]);
  } catch (e) { next(e); }
});

app.get('/api/admin/users', auth, guard('ROLE_ADMIN'), async (req, res, next) => {
  try {
    const [r] = await pool.query(`SELECT u.*,sp.college_name student_college_name,op.catering_name owner_catering_name
      FROM users u LEFT JOIN student_profiles sp ON sp.user_id=u.id
      LEFT JOIN owner_profiles op ON op.user_id=u.id ORDER BY u.created_at DESC`);
    ok(res, r.map(x => ({
      id: x.id, email: x.email, fullName: x.full_name, phone: x.phone,
      role: x.role, active: !!x.is_active, suspended: !!x.is_suspended,
      createdAt: x.created_at,
      collegeName: x.student_college_name || null,
      cateringName: x.owner_catering_name || null
    })));
  } catch (e) { next(e); }
});

async function adminProfiles(req, res, next, t) {
  try {
    const [r] = await pool.query(`SELECT u.*,p.* FROM users u JOIN ${t === 'student' ? 'student_profiles' : 'owner_profiles'} p ON p.user_id=u.id`);
    ok(res, r.map(profile));
  } catch (e) { next(e); }
}

app.get('/api/admin/students', auth, guard('ROLE_ADMIN'), (q, s, n) => adminProfiles(q, s, n, 'student'));
app.get('/api/admin/owners', auth, guard('ROLE_ADMIN'), (q, s, n) => adminProfiles(q, s, n, 'owner'));

app.put('/api/admin/owners/:id/verify', auth, guard('ROLE_ADMIN'), async (req, res, next) => {
  try {
    const v = req.query.verified !== 'false';
    await pool.query('UPDATE owner_profiles SET verification_status=?,verified_at=? WHERE id=?',
      [v ? 'VERIFIED' : 'PENDING_VERIFICATION', v ? new Date() : null, req.params.id]);
    const [r] = await pool.query('SELECT u.*,p.* FROM users u JOIN owner_profiles p ON p.user_id=u.id WHERE p.id=?', [req.params.id]);
    ok(res, profile(r[0]), v ? 'Owner verified successfully!' : 'Owner verification reverted to pending.');
  } catch (e) { next(e); }
});

app.put('/api/admin/users/:id/suspend', auth, guard('ROLE_ADMIN'), async (req, res, next) => {
  try {
    const v = req.query.suspended === 'true';
    await pool.query('UPDATE users SET is_suspended=? WHERE id=? AND role<>"ROLE_ADMIN"', [v, req.params.id]);
    const [r] = await pool.query('SELECT * FROM users WHERE id=?', [req.params.id]);
    ok(res, r[0], v ? 'User suspended successfully' : 'User reactivated successfully');
  } catch (e) { next(e); }
});

app.get('/api/admin/jobs', auth, guard('ROLE_ADMIN'), async (req, res, next) => {
  try {
    const [r] = await pool.query(`${jobSql} ORDER BY j.created_at DESC`);
    ok(res, r.map(x => job(x, true)));
  } catch (e) { next(e); }
});

app.delete('/api/admin/jobs/:id', auth, guard('ROLE_ADMIN'), async (req, res, next) => {
  try {
    await pool.query("UPDATE catering_jobs SET status='CANCELLED' WHERE id=?", [req.params.id]);
    ok(res, null, 'Job deleted/cancelled by admin');
  } catch (e) { next(e); }
});

// ─── NOTIFICATIONS ───────────────────────────────────────────────────────────

app.get('/api/notifications', auth, async (req, res, next) => {
  try {
    const [r] = await pool.query('SELECT * FROM notifications WHERE recipient_id=? ORDER BY created_at DESC', [req.user.id]);
    ok(res, r.map(x => ({...x, isRead: !!x.is_read})));
  } catch (e) { next(e); }
});

app.get('/api/notifications/unread-count', auth, async (req, res, next) => {
  try {
    const [r] = await pool.query('SELECT COUNT(*) count FROM notifications WHERE recipient_id=? AND is_read=FALSE', [req.user.id]);
    ok(res, r[0].count);
  } catch (e) { next(e); }
});

app.put('/api/notifications/:id/read', auth, async (req, res, next) => {
  try {
    await pool.query('UPDATE notifications SET is_read=TRUE WHERE id=? AND recipient_id=?', [req.params.id, req.user.id]);
    ok(res, null, 'Notification marked as read');
  } catch (e) { next(e); }
});

app.put('/api/notifications/read-all', auth, async (req, res, next) => {
  try {
    await pool.query('UPDATE notifications SET is_read=TRUE WHERE recipient_id=?', [req.user.id]);
    ok(res, null, 'All notifications marked as read');
  } catch (e) { next(e); }
});

// ─── REPORTS / COMPLAINTS ───────────────────────────────────────────────────

app.post('/api/reports', auth, async (req, res, next) => {
  try {
    const b = req.body;
    const result = await transaction(async c => {
      const [[target]] = await c.query('SELECT ou.id owner_user_id,j.title FROM job_applications a JOIN student_profiles sp ON sp.id=a.student_id JOIN catering_jobs j ON j.id=a.job_id JOIN owner_profiles op ON op.id=j.owner_id JOIN users ou ON ou.id=op.user_id WHERE a.id=? AND sp.user_id=?',
        [b.applicationId, req.user.id]);
      if (!target) throw Object.assign(new Error('Application not found or not owned by student'), {status: 404});
      const [created] = await c.query('INSERT INTO reports (reporter_id,target_user_id,job_id,application_id,report_type,description,expected_amount,received_amount,evidence_notes) VALUES (?,?,?,?,?,?,?,?,?)',
        [req.user.id, target.owner_user_id, b.jobId || null, b.applicationId || null, b.reportType, b.description, b.expectedAmount || null, b.receivedAmount || null, b.evidenceNotes || null]);
      await c.query("INSERT INTO notifications (recipient_id,title,message,type,related_entity_id) VALUES (?,?,?,?,?)",
        [target.owner_user_id, 'Student Complaint Received', `A student submitted a complaint about "${target.title}". Please review it and cooperate with platform administrators.`, 'REPORT_SUBMITTED', created.insertId]);
      return created.insertId;
    });
    ok(res, {id: result}, 'Report submitted to the owner and platform administrators.');
  } catch (e) { next(e); }
});

app.get('/api/reports/my-reports', auth, async (req, res, next) => {
  try {
    const [r] = await pool.query('SELECT * FROM reports WHERE reporter_id=?', [req.user.id]);
    ok(res, r);
  } catch (e) { next(e); }
});

// FIXED: Admin reports endpoint now includes target user details (name, email, catering name)
app.get('/api/admin/reports', auth, guard('ROLE_ADMIN'), async (req, res, next) => {
  try {
    const [r] = await pool.query(`SELECT r.*,
      u.full_name reporter_name, u.email reporter_email,
      tu.full_name target_user_name, tu.email target_user_email,
      op.catering_name target_catering_name
      FROM reports r
      JOIN users u ON u.id=r.reporter_id
      JOIN users tu ON tu.id=r.target_user_id
      LEFT JOIN owner_profiles op ON op.user_id=tu.id
      ORDER BY r.created_at DESC`);
    ok(res, r.map(x => ({
      ...x,
      reporterName: x.reporter_name,
      reporterEmail: x.reporter_email,
      targetUserName: x.target_user_name,
      targetUserEmail: x.target_user_email,
      targetCateringName: x.target_catering_name,
      reportTypeDisplayName: x.report_type
    })));
  } catch (e) { next(e); }
});

app.put('/api/admin/reports/:id/resolve', auth, guard('ROLE_ADMIN'), async (req, res, next) => {
  try {
    await pool.query('UPDATE reports SET status=?,admin_remarks=?,resolved_at=NOW() WHERE id=?',
      [req.body.status, req.body.adminRemarks || null, req.params.id]);
    const [r] = await pool.query('SELECT * FROM reports WHERE id=?', [req.params.id]);
    ok(res, r[0], 'Report resolved successfully');
  } catch (e) { next(e); }
});

app.delete('/api/reports/:id', auth, async (req, res, next) => {
  try {
    const result = await transaction(async c => {
      const [[report]] = await c.query('SELECT id,target_user_id FROM reports WHERE id=? AND reporter_id=? FOR UPDATE',
        [req.params.id, req.user.id]);
      if (!report) throw Object.assign(new Error('Complaint not found'), {status: 404});
      await c.query('DELETE FROM reports WHERE id=? AND reporter_id=?', [req.params.id, req.user.id]);
      await c.query("INSERT INTO notifications (recipient_id,title,message,type,related_entity_id) VALUES (?,?,?,?,?)",
        [report.target_user_id, 'Complaint Withdrawn', 'The student withdrew the complaint. It has been removed from your complaints list.', 'REPORT_WITHDRAWN', report.id]);
      return report.id;
    });
    ok(res, {id: result}, 'Complaint withdrawn successfully.');
  } catch (e) { next(e); }
});

// ─── FILE UPLOAD ─────────────────────────────────────────────────────────────

app.post('/api/upload/image', auth, upload.single('image'), (req, res, next) => {
  try {
    if (!req.file) {
      console.error('[UPLOAD] No file received. Body keys:', Object.keys(req.body || {}));
      return fail(res, 400, 'No image file uploaded. Please select an image file under 5MB.');
    }
    const url = `/uploads/${req.file.filename}`;
    console.log('[UPLOAD] File saved:', req.file.filename, 'URL:', url);
    ok(res, {url}, 'Image uploaded successfully');
  } catch (e) {
    console.error('[UPLOAD] Error:', e.message);
    next(e);
  }
});
// Multer + general error handler
app.use((err, req, res, next) => {
  if (err instanceof multer.MulterError) {
    console.error('[UPLOAD] Multer error:', err.code, err.message);
    if (err.code === 'LIMIT_FILE_SIZE') return fail(res, 400, 'File too large. Maximum size is 5MB.');
    return fail(res, 400, 'Upload failed: ' + err.message);
  }
  if (err.message && err.message.includes('image')) {
    console.error('[UPLOAD] Image filter error:', err.message);
    return fail(res, 400, err.message);
  }
  next(err);
});

// ─── FORGOT PASSWORD ───────────────────────────────────────────────────────

const resetChallenges = new Map();

app.post('/api/auth/forgot-password', async (req, res, next) => {
  try {
    const email = String(req.body.email || '').trim().toLowerCase();
    if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return fail(res, 400, 'Enter a valid email address');
    const [r] = await pool.query('SELECT id, email FROM users WHERE email=?', [email]);
    if (!r[0]) return fail(res, 404, 'No account found with that email');
    const id = crypto.randomUUID();
    const code = String(crypto.randomInt(100000, 1000000));
    await sendEmailOtp(email, code);
    resetChallenges.set(id, {email, code, expiresAt: Date.now() + 10 * 60 * 1000, attempts: 0});
    ok(res, {verificationId: id, expiresInSeconds: 600}, 'Password reset code sent to your email');
  } catch (e) { next(e); }
});

app.post('/api/auth/forgot-password/verify', async (req, res, next) => {
  try {
    const challenge = resetChallenges.get(req.body.verificationId);
    const otp = String(req.body.otp || '');
    if (!challenge || challenge.expiresAt < Date.now()) return fail(res, 400, 'Request a new reset code');
    if (!/^\d{6}$/.test(otp)) return fail(res, 400, 'OTP must contain exactly 6 digits');
    if (challenge.attempts >= 5) return fail(res, 429, 'Too many incorrect codes. Request a new code');
    if (otp !== challenge.code) { challenge.attempts++; return fail(res, 400, 'Incorrect verification code'); }
    challenge.verified = true;
    ok(res, null, 'Email verified successfully');
  } catch (e) { next(e); }
});

app.post('/api/auth/forgot-password/reset', async (req, res, next) => {
  try {
    const challenge = resetChallenges.get(req.body.verificationId);
    if (!challenge || !challenge.verified) return fail(res, 400, 'Please verify your email first');
    const newPassword = String(req.body.password || '');
    if (!newPassword || newPassword.length < 6) return fail(res, 400, 'Password must be at least 6 characters');
    const hash = await bcrypt.hash(newPassword, 10);
    await pool.query('UPDATE users SET password_hash=? WHERE email=?', [hash, challenge.email]);
    resetChallenges.delete(req.body.verificationId);
    ok(res, null, 'Password reset successful! You can now sign in with your new password.');
  } catch (e) { next(e); }
});

// ─── DELETE ACCOUNT ─────────────────────────────────────────────────────────

app.delete('/api/account', auth, async (req, res, next) => {
  try {
    const password = String(req.body.password || '');
    if (!password) return fail(res, 400, 'Password is required to delete your account');
    const [r] = await pool.query('SELECT id, password_hash FROM users WHERE id=?', [req.user.id]);
    if (!r[0] || !(await bcrypt.compare(password, r[0].password_hash)))
      return fail(res, 401, 'Incorrect password');
    // Delete user (cascading deletes handle profiles, jobs, applications, etc.)
    await pool.query('DELETE FROM users WHERE id=? AND role<>"ROLE_ADMIN"', [req.user.id]);
    ok(res, null, 'Account deleted successfully');
  } catch (e) { next(e); }
});

// ─── STATIC FILES & ERROR HANDLING ───────────────────────────────────────────

app.use('/uploads', express.static(uploadsDir));
app.use(express.static(staticDir));
app.get('*', (req, res) => res.sendFile(path.join(staticDir, 'index.html')));
app.use((e, req, res, next) => { console.error(e); fail(res, e.status || 500, e.status ? e.message : 'Internal server error'); });

// ─── START ───────────────────────────────────────────────────────────────────

if (require.main === module) {
  initializeDatabase().then(() => app.listen(PORT, '0.0.0.0', () => console.log(`PartTime Job Platform listening on http://0.0.0.0:${PORT}`)))
    .catch(e => { console.error('Database initialization failed:', e.message); process.exitCode = 1; });
}

module.exports = app;
