const express = require('express');
const router = express.Router();
const {pool} = require('./db');

async function complaintParticipant(req, reportId) {
  const [rows] = await pool.query('SELECT id,reporter_id,target_user_id FROM reports WHERE id=? AND (reporter_id=? OR target_user_id=?)', [reportId, req.user.id, req.user.id]);
  return rows[0];
}

router.get('/:reportId/messages', async (req, res, next) => {
  try {
    const report = await complaintParticipant(req, req.params.reportId);
    if (!report) return res.status(404).json({message: 'Complaint not found'});
    const [rows] = await pool.query('SELECT m.id,m.report_id,m.sender_id,m.message,m.created_at,u.full_name sender_name,u.role sender_role FROM complaint_messages m JOIN users u ON u.id=m.sender_id WHERE m.report_id=? ORDER BY m.created_at ASC', [report.id]);
    res.json({data: rows.map(row => ({id: row.id, reportId: row.report_id, senderId: row.sender_id, senderName: row.sender_name, senderRole: row.sender_role, message: row.message, createdAt: row.created_at}))});
  } catch (error) { next(error); }
});

router.post('/:reportId/messages', async (req, res, next) => {
  try {
    const message = String(req.body.message || '').trim();
    if (!message) return res.status(400).json({message: 'Message cannot be empty'});
    if (message.length > 2000) return res.status(400).json({message: 'Message is too long'});
    const report = await complaintParticipant(req, req.params.reportId);
    if (!report) return res.status(404).json({message: 'Complaint not found'});
    const [created] = await pool.query('INSERT INTO complaint_messages (report_id,sender_id,message) VALUES (?,?,?)', [report.id, req.user.id, message]);
    const [rows] = await pool.query('SELECT m.id,m.report_id,m.sender_id,m.message,m.created_at,u.full_name sender_name,u.role sender_role FROM complaint_messages m JOIN users u ON u.id=m.sender_id WHERE m.id=?', [created.insertId]);
    const row = rows[0];
    res.status(201).json({data: {id: row.id, reportId: row.report_id, senderId: row.sender_id, senderName: row.sender_name, senderRole: row.sender_role, message: row.message, createdAt: row.created_at}, message: 'Message sent'});
  } catch (error) { next(error); }
});

module.exports = router;
