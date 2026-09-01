const express = require('express');
const crypto = require('crypto');
const Razorpay = require('razorpay');
const {pool, transaction} = require('./db');

const router = express.Router();

// ─── Razorpay client (lazy init) ──────────────────────────────────────────────
let razorpay = null;
function getRazorpay() {
  if (razorpay) return razorpay;
  const keyId = process.env.RAZORPAY_KEY_ID;
  const keySecret = process.env.RAZORPAY_KEY_SECRET;
  if (!keyId || !keySecret) throw Object.assign(new Error('Razorpay is not configured. Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET.'), {status: 503});
  razorpay = new Razorpay({key_id: keyId, key_secret: keySecret});
  return razorpay;
}

// ─── Helper: verify Razorpay signature ────────────────────────────────────────
function verifySignature(orderId, paymentId, signature) {
  const keySecret = process.env.RAZORPAY_KEY_SECRET;
  if (!keySecret) return false;
  const body = orderId + '|' + paymentId;
  const expectedSig = crypto.createHmac('sha256', keySecret).update(body).digest('hex');
  return expectedSig === signature;
}

// ─── Helper: get auth middleware (reuse from parent server) ────────────────────
// We export these routes expecting auth middleware to be applied in parent

// ─── GET /api/public/razorpay-key ─────────────────────────────────────────────
// Returns Razorpay key_id for frontend checkout (never expose secret)
router.get('/api/public/razorpay-key', (req, res) => {
  const keyId = process.env.RAZORPAY_KEY_ID || '';
  res.json({data: {keyId, configured: !!keyId}});
});

// ─── POST /api/student/razorpay/create-order ──────────────────────────────────
// Creates a Razorpay order for a payment record
router.post('/api/student/razorpay/create-order', async (req, res) => {
  try {
    const {paymentRecordId} = req.body;
    if (!paymentRecordId) return res.status(400).json({message: 'paymentRecordId is required'});

    const rp = getRazorpay();

    // Get payment record and validate ownership
    const [[record]] = await pool.query(
      `SELECT p.*, a.status as app_status FROM payment_records p
       JOIN job_applications a ON a.id = p.application_id
       JOIN student_profiles sp ON sp.id = p.student_id
       WHERE p.id = ? AND sp.user_id = ?`,
      [paymentRecordId, req.user.id]
    );

    if (!record) return res.status(404).json({message: 'Payment record not found'});
    if (record.app_status !== 'ACCEPTED') return res.status(400).json({message: 'Application is not accepted'});
    if (record.payment_status === 'SUCCESS' || record.payment_status === 'CONFIRMED')
      return res.status(400).json({message: 'This job is already paid for'});

    // Check if there's already a CREATED order that hasn't expired (within 15 min)
    if (record.razorpay_order_id && record.payment_status === 'CREATED') {
      const orderAge = Date.now() - new Date(record.created_at).getTime();
      if (orderAge < 15 * 60 * 1000) {
        // Return existing order
        return res.json({data: {
          orderId: record.razorpay_order_id,
          amount: Number(record.amount) * 100,
          currency: 'INR',
          paymentRecordId: record.id,
          jobTitle: record.payment_type
        }});
      }
    }

    // Get job title for the receipt
    const [[job]] = await pool.query('SELECT title FROM catering_jobs WHERE id = ?', [record.job_id]);
    const amountPaise = Math.round(Number(record.amount) * 100);

    // Create Razorpay order
    const order = await rp.orders.create({
      amount: amountPaise,
      currency: 'INR',
      receipt: `ptjob_pr${record.id}_j${record.job_id}`,
      notes: {jobId: String(record.job_id), studentId: String(record.student_id)}
    });

    // Update payment record with order ID
    await pool.query(
      "UPDATE payment_records SET razorpay_order_id = ?, payment_status = 'CREATED', environment = 'TEST' WHERE id = ?",
      [order.id, record.id]
    );

    res.json({data: {
      orderId: order.id,
      amount: amountPaise,
      currency: 'INR',
      paymentRecordId: record.id,
      jobTitle: job ? job.title : 'Job'
    }});
  } catch (e) {
    console.error('[RAZORPAY] create-order error:', e.message);
    res.status(e.status || 500).json({message: e.message || 'Failed to create payment order'});
  }
});

// ─── POST /api/student/razorpay/verify ────────────────────────────────────────
// Verifies Razorpay payment signature and updates transaction
router.post('/api/student/razorpay/verify', async (req, res) => {
  try {
    const {razorpay_order_id, razorpay_payment_id, razorpay_signature, paymentRecordId} = req.body;

    if (!razorpay_order_id || !razorpay_payment_id || !razorpay_signature || !paymentRecordId)
      return res.status(400).json({message: 'Missing required payment verification fields'});

    // Verify signature
    if (!verifySignature(razorpay_order_id, razorpay_payment_id, razorpay_signature))
      return res.status(400).json({message: 'Invalid payment signature'});

    // Get payment record and validate
    const [[record]] = await pool.query(
      `SELECT p.* FROM payment_records p
       JOIN student_profiles sp ON sp.id = p.student_id
       WHERE p.id = ? AND sp.user_id = ?`,
      [paymentRecordId, req.user.id]
    );

    if (!record) return res.status(404).json({message: 'Payment record not found'});
    if (record.payment_status === 'SUCCESS' || record.payment_status === 'CONFIRMED')
      return res.status(400).json({message: 'Already verified'});

    // Verify the order_id matches
    if (record.razorpay_order_id !== razorpay_order_id)
      return res.status(400).json({message: 'Order ID mismatch'});

    // Try to fetch payment details from Razorpay to get method
    let paymentMethod = null;
    try {
      const rp = getRazorpay();
      const payment = await rp.payments.fetch(razorpay_payment_id);
      paymentMethod = payment.method || null;
    } catch (e) {
      console.error('[RAZORPAY] Could not fetch payment details:', e.message);
    }

    // Update in transaction
    await transaction(async c => {
      await c.query(
        `UPDATE payment_records SET
          razorpay_payment_id = ?,
          razorpay_signature = ?,
          payment_status = 'SUCCESS',
          payment_method = ?,
          confirmed_paid_at = NOW()
         WHERE id = ?`,
        [razorpay_payment_id, razorpay_signature, paymentMethod, record.id]
      );
      await c.query(
        "UPDATE job_applications SET payment_status = 'SUCCESS', payment_confirmation_date = NOW() WHERE id = ?",
        [record.application_id]
      );
    });

    res.json({data: {
      success: true,
      paymentRecordId: record.id,
      razorpay_payment_id,
      paymentMethod
    }, message: 'Payment verified successfully!'});
  } catch (e) {
    console.error('[RAZORPAY] verify error:', e.message);
    res.status(e.status || 500).json({message: e.message || 'Payment verification failed'});
  }
});

// ─── POST /api/student/razorpay/cancel ────────────────────────────────────────
// Marks a CREATED payment as CANCELLED (user closed checkout)
router.post('/api/student/razorpay/cancel', async (req, res) => {
  try {
    const {paymentRecordId} = req.body;
    if (!paymentRecordId) return res.status(400).json({message: 'paymentRecordId is required'});

    const [[record]] = await pool.query(
      `SELECT p.* FROM payment_records p
       JOIN student_profiles sp ON sp.id = p.student_id
       WHERE p.id = ? AND sp.user_id = ?`,
      [paymentRecordId, req.user.id]
    );

    if (!record) return res.status(404).json({message: 'Payment record not found'});
    if (record.payment_status !== 'CREATED')
      return res.status(400).json({message: 'Only pending orders can be cancelled'});

    await pool.query("UPDATE payment_records SET payment_status = 'CANCELLED' WHERE id = ?", [record.id]);
    res.json({data: null, message: 'Payment cancelled'});
  } catch (e) {
    console.error('[RAZORPAY] cancel error:', e.message);
    res.status(500).json({message: 'Failed to cancel payment'});
  }
});

// ─── POST /api/razorpay/webhook ───────────────────────────────────────────────
// Razorpay webhook handler (no auth required, uses signature verification)
router.post('/api/razorpay/webhook', express.raw({type: 'application/json'}), async (req, res) => {
  try {
    const webhookSecret = process.env.RAZORPAY_WEBHOOK_SECRET;
    if (!webhookSecret) {
      console.warn('[RAZORPAY] Webhook secret not configured, skipping verification');
    }

    // Verify webhook signature if secret is set
    if (webhookSecret) {
      const signature = req.headers['x-razorpay-signature'];
      const body = typeof req.body === 'string' ? req.body : JSON.stringify(req.body);
      const expectedSig = crypto.createHmac('sha256', webhookSecret).update(body).digest('hex');
      if (signature !== expectedSig) {
        console.error('[RAZORPAY] Invalid webhook signature');
        return res.status(400).json({message: 'Invalid signature'});
      }
    }

    const event = typeof req.body === 'string' ? JSON.parse(req.body) : req.body;
    console.log('[RAZORPAY] Webhook received:', event.event);

    if (event.event === 'payment.captured') {
      const payment = event.payload.payment.entity;
      const orderId = payment.order_id;
      const paymentId = payment.id;
      const method = payment.method;

      // Find and update the payment record (idempotent)
      const [[record]] = await pool.query(
        "SELECT * FROM payment_records WHERE razorpay_order_id = ? AND payment_status != 'SUCCESS' LIMIT 1",
        [orderId]
      );

      if (record) {
        await transaction(async c => {
          await c.query(
            `UPDATE payment_records SET
              razorpay_payment_id = COALESCE(?, razorpay_payment_id),
              payment_status = 'SUCCESS',
              payment_method = ?,
              confirmed_paid_at = NOW()
             WHERE id = ? AND payment_status != 'SUCCESS'`,
            [paymentId, method, record.id]
          );
          await c.query(
            "UPDATE job_applications SET payment_status = 'SUCCESS', payment_confirmation_date = NOW() WHERE id = ? AND payment_status != 'SUCCESS'",
            [record.application_id]
          );
        });
        console.log('[RAZORPAY] Webhook: payment confirmed for record', record.id);
      }
    } else if (event.event === 'payment.failed') {
      const payment = event.payload.payment.entity;
      const orderId = payment.order_id;
      await pool.query(
        "UPDATE payment_records SET payment_status = 'FAILED' WHERE razorpay_order_id = ? AND payment_status = 'CREATED'",
        [orderId]
      );
      console.log('[RAZORPAY] Webhook: payment failed for order', orderId);
    }

    res.json({status: 'ok'});
  } catch (e) {
    console.error('[RAZORPAY] Webhook error:', e.message);
    res.status(500).json({message: 'Webhook processing error'});
  }
});

// ─── GET /api/student/razorpay/transactions ───────────────────────────────────
// Student transaction history with Razorpay details
router.get('/api/student/razorpay/transactions', async (req, res) => {
  try {
    const [rows] = await pool.query(
      `SELECT p.*, j.title job_title, j.work_area, o.catering_name owner_catering_name,
              a.status app_status
       FROM payment_records p
       JOIN catering_jobs j ON j.id = p.job_id
       JOIN student_profiles s ON s.id = p.student_id
       JOIN owner_profiles o ON o.id = p.owner_id
       JOIN job_applications a ON a.id = p.application_id
       WHERE s.user_id = ?
       ORDER BY p.created_at DESC`,
      [req.user.id]
    );
    res.json({data: rows.map(r => ({
      id: r.id, applicationId: r.application_id, jobId: r.job_id,
      amount: r.amount, paymentType: r.payment_type,
      paymentStatus: r.payment_status,
      razorpayOrderId: r.razorpay_order_id, razorpayPaymentId: r.razorpay_payment_id,
      environment: r.environment, paymentMethod: r.payment_method,
      createdAt: r.created_at, confirmedPaidAt: r.confirmed_paid_at,
      jobTitle: r.job_title, workArea: r.work_area,
      ownerCateringName: r.owner_catering_name,
      appStatus: r.app_status
    }))});
  } catch (e) {
    console.error('[RAZORPAY] transactions error:', e.message);
    res.status(500).json({message: 'Failed to fetch transactions'});
  }
});

// ─── GET /api/owner/razorpay/transactions ─────────────────────────────────────
// Owner transaction history
router.get('/api/owner/razorpay/transactions', async (req, res) => {
  try {
    const [[op]] = await pool.query('SELECT id FROM owner_profiles WHERE user_id = ?', [req.user.id]);
    if (!op) return res.json({data: []});

    const [rows] = await pool.query(
      `SELECT p.*, j.title job_title, j.work_area,
              su.full_name student_name, su.email student_email
       FROM payment_records p
       JOIN catering_jobs j ON j.id = p.job_id
       JOIN student_profiles sp ON sp.id = p.student_id
       JOIN users su ON su.id = sp.user_id
       WHERE p.owner_id = ?
       ORDER BY p.created_at DESC`,
      [op.id]
    );
    res.json({data: rows.map(r => ({
      id: r.id, applicationId: r.application_id, jobId: r.job_id,
      amount: r.amount, paymentType: r.payment_type,
      paymentStatus: r.payment_status,
      razorpayOrderId: r.razorpay_order_id, razorpayPaymentId: r.razorpay_payment_id,
      environment: r.environment, paymentMethod: r.payment_method,
      createdAt: r.created_at, confirmedPaidAt: r.confirmed_paid_at,
      markedPaidAt: r.marked_paid_at,
      jobTitle: r.job_title, workArea: r.work_area,
      studentName: r.student_name, studentEmail: r.student_email
    }))});
  } catch (e) {
    console.error('[RAZORPAY] owner transactions error:', e.message);
    res.status(500).json({message: 'Failed to fetch transactions'});
  }
});

module.exports = router;
