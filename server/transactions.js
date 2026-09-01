const {pool, transaction} = require('./db');

// Generate unique transaction ID: TXN-YYYYMMDD-XXXXX
async function generateTransactionId() {
  const today = new Date().toISOString().slice(0, 10).replace(/-/g, '');
  const [[result]] = await pool.query(
    `SELECT COALESCE(MAX(CAST(SUBSTRING(transaction_id, -5) AS UNSIGNED)), 0) + 1 as next_id 
     FROM payment_records 
     WHERE transaction_id LIKE ?`,
    [`TXN-${today}-%`]
  );
  const id = String(result.next_id).padStart(5, '0');
  return `TXN-${today}-${id}`;
}

// Create or update transaction when payment is initiated
async function createOrUpdateTransaction(paymentRecord) {
  try {
    // Check if transaction exists for this razorpay_order_id
    const [[existing]] = await pool.query(
      'SELECT id, transaction_id FROM payment_records WHERE razorpay_order_id = ? LIMIT 1',
      [paymentRecord.razorpay_order_id]
    );

    if (existing && existing.transaction_id) {
      // Transaction already exists, just update status if needed
      return existing.transaction_id;
    }

    // Generate new transaction ID
    const txnId = await generateTransactionId();

    // Update the payment record with transaction ID
    await pool.query(
      'UPDATE payment_records SET transaction_id = ? WHERE razorpay_order_id = ?',
      [txnId, paymentRecord.razorpay_order_id]
    );

    return txnId;
  } catch (e) {
    console.error('[TRANSACTION] Error creating/updating transaction:', e.message);
    throw e;
  }
}

// Update transaction status
async function updateTransactionStatus(razorpayOrderId, status, paymentId, failureReason = null) {
  try {
    const [[record]] = await pool.query(
      'SELECT id, transaction_id FROM payment_records WHERE razorpay_order_id = ? LIMIT 1',
      [razorpayOrderId]
    );

    if (!record) {
      console.error('[TRANSACTION] No payment record found for order:', razorpayOrderId);
      return null;
    }

    // Update payment status and failure reason if applicable
    const updates = {
      payment_status: status,
      razorpay_payment_id: paymentId,
      updated_at: new Date()
    };

    if (failureReason) {
      updates.failure_reason = failureReason;
    }

    if (status === 'SUCCESS') {
      updates.confirmed_paid_at = new Date();
      updates.marked_paid_at = new Date();
    }

    let sql = 'UPDATE payment_records SET ';
    const values = [];
    Object.entries(updates).forEach(([key, value], index) => {
      if (index > 0) sql += ', ';
      sql += `${key} = ?`;
      values.push(value);
    });
    sql += ' WHERE id = ?';
    values.push(record.id);

    await pool.query(sql, values);

    return record.transaction_id;
  } catch (e) {
    console.error('[TRANSACTION] Error updating transaction status:', e.message);
    throw e;
  }
}

// Get transaction with full details
async function getTransactionDetails(transactionId) {
  try {
    const [[txn]] = await pool.query(`
      SELECT 
        p.*,
        j.title job_title,
        j.work_area,
        j.work_type,
        su.full_name student_name,
        su.email student_email,
        su.id student_user_id,
        sp.college_name,
        ou.full_name owner_name,
        ou.email owner_email,
        ou.id owner_user_id,
        o.catering_name,
        a.status application_status
      FROM payment_records p
      JOIN catering_jobs j ON j.id = p.job_id
      JOIN student_profiles sp ON sp.id = p.student_id
      JOIN users su ON su.id = sp.user_id
      JOIN owner_profiles o ON o.id = p.owner_id
      JOIN users ou ON ou.id = o.user_id
      JOIN job_applications a ON a.id = p.application_id
      WHERE p.transaction_id = ?
    `, [transactionId]);

    if (!txn) return null;

    return formatTransaction(txn);
  } catch (e) {
    console.error('[TRANSACTION] Error getting transaction details:', e.message);
    throw e;
  }
}

// Format transaction for API response
function formatTransaction(txn) {
  return {
    transactionId: txn.transaction_id,
    paymentRecordId: txn.id,
    applicationId: txn.application_id,
    jobId: txn.job_id,
    jobTitle: txn.job_title,
    workArea: txn.work_area,
    workType: txn.work_type,
    studentId: txn.student_id,
    studentName: txn.student_name,
    studentEmail: txn.student_email,
    studentUserId: txn.student_user_id,
    collegeName: txn.college_name,
    ownerId: txn.owner_id,
    ownerName: txn.owner_name,
    ownerEmail: txn.owner_email,
    ownerUserId: txn.owner_user_id,
    cateringName: txn.catering_name,
    amount: txn.amount,
    currency: txn.currency || 'INR',
    paymentType: txn.payment_type,
    paymentStatus: txn.payment_status,
    razorpayOrderId: txn.razorpay_order_id,
    razorpayPaymentId: txn.razorpay_payment_id,
    paymentMethod: txn.payment_method,
    failureReason: txn.failure_reason,
    environment: txn.environment,
    applicationStatus: txn.application_status,
    createdAt: txn.created_at,
    initiatedAt: txn.initiated_at,
    markedPaidAt: txn.marked_paid_at,
    confirmedPaidAt: txn.confirmed_paid_at,
    updatedAt: txn.updated_at
  };
}

// Get transactions with filtering and pagination
async function getTransactionsList(filters = {}) {
  try {
    let sql = `
      SELECT 
        p.*,
        j.title job_title,
        j.work_area,
        su.full_name student_name,
        ou.full_name owner_name,
        o.catering_name
      FROM payment_records p
      JOIN catering_jobs j ON j.id = p.job_id
      JOIN student_profiles sp ON sp.id = p.student_id
      JOIN users su ON su.id = sp.user_id
      JOIN owner_profiles o ON o.id = p.owner_id
      JOIN users ou ON ou.id = o.user_id
      WHERE 1=1
    `;
    const values = [];

    // Apply filters
    if (filters.status) {
      sql += ' AND p.payment_status = ?';
      values.push(filters.status);
    }

    if (filters.environment) {
      sql += ' AND p.environment = ?';
      values.push(filters.environment);
    }

    if (filters.studentId) {
      sql += ' AND p.student_id = ?';
      values.push(filters.studentId);
    }

    if (filters.ownerId) {
      sql += ' AND p.owner_id = ?';
      values.push(filters.ownerId);
    }

    if (filters.jobId) {
      sql += ' AND p.job_id = ?';
      values.push(filters.jobId);
    }

    if (filters.fromDate) {
      sql += ' AND DATE(p.created_at) >= ?';
      values.push(filters.fromDate);
    }

    if (filters.toDate) {
      sql += ' AND DATE(p.created_at) <= ?';
      values.push(filters.toDate);
    }

    if (filters.searchQuery) {
      const query = `%${filters.searchQuery}%`;
      sql += ' AND (p.transaction_id LIKE ? OR su.full_name LIKE ? OR ou.full_name LIKE ? OR j.title LIKE ? OR p.razorpay_order_id LIKE ? OR p.razorpay_payment_id LIKE ?)';
      values.push(query, query, query, query, query, query);
    }

    // Add sorting
    sql += ' ORDER BY p.created_at DESC';

    // Add pagination
    const limit = parseInt(filters.limit) || 50;
    const offset = parseInt(filters.offset) || 0;
    sql += ' LIMIT ? OFFSET ?';
    values.push(limit, offset + 1);

    const [transactions] = await pool.query(sql, values);

    // Get total count
    let countSql = 'SELECT COUNT(*) as total FROM payment_records p WHERE 1=1';
    const countValues = [];

    if (filters.status) {
      countSql += ' AND p.payment_status = ?';
      countValues.push(filters.status);
    }
    if (filters.environment) {
      countSql += ' AND p.environment = ?';
      countValues.push(filters.environment);
    }
    if (filters.studentId) {
      countSql += ' AND p.student_id = ?';
      countValues.push(filters.studentId);
    }
    if (filters.ownerId) {
      countSql += ' AND p.owner_id = ?';
      countValues.push(filters.ownerId);
    }
    if (filters.jobId) {
      countSql += ' AND p.job_id = ?';
      countValues.push(filters.jobId);
    }
    if (filters.fromDate) {
      countSql += ' AND DATE(p.created_at) >= ?';
      countValues.push(filters.fromDate);
    }
    if (filters.toDate) {
      countSql += ' AND DATE(p.created_at) <= ?';
      countValues.push(filters.toDate);
    }

    const [[countResult]] = await pool.query(countSql, countValues);

    return {
      transactions: transactions.map(t => formatTransaction(t)),
      total: countResult.total,
      limit,
      offset
    };
  } catch (e) {
    console.error('[TRANSACTION] Error getting transactions list:', e.message);
    throw e;
  }
}

// Get transaction summary statistics
async function getTransactionSummary(filters = {}) {
  try {
    let sql = `
      SELECT
        COUNT(*) as totalTransactions,
        COUNT(CASE WHEN payment_status = 'SUCCESS' THEN 1 END) as successfulTransactions,
        COUNT(CASE WHEN payment_status = 'PENDING' THEN 1 END) as pendingTransactions,
        COUNT(CASE WHEN payment_status = 'FAILED' THEN 1 END) as failedTransactions,
        COUNT(CASE WHEN payment_status = 'CANCELLED' THEN 1 END) as cancelledTransactions,
        COALESCE(SUM(CASE WHEN payment_status IN ('SUCCESS', 'CONFIRMED', 'PAID') THEN amount ELSE 0 END), 0) as totalSuccessfulAmount,
        COALESCE(SUM(CASE WHEN payment_status = 'PENDING' THEN amount ELSE 0 END), 0) as totalPendingAmount,
        COALESCE(SUM(CASE WHEN environment = 'TEST' THEN amount ELSE 0 END), 0) as totalTestAmount
      FROM payment_records
      WHERE environment = 'TEST'
    `;
    const values = [];

    if (filters.fromDate) {
      sql += ' AND DATE(created_at) >= ?';
      values.push(filters.fromDate);
    }

    if (filters.toDate) {
      sql += ' AND DATE(created_at) <= ?';
      values.push(filters.toDate);
    }

    const [[summary]] = await pool.query(sql, values);
    return summary;
  } catch (e) {
    console.error('[TRANSACTION] Error getting summary:', e.message);
    throw e;
  }
}

module.exports = {
  generateTransactionId,
  createOrUpdateTransaction,
  updateTransactionStatus,
  getTransactionDetails,
  formatTransaction,
  getTransactionsList,
  getTransactionSummary
};
