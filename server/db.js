const fs = require('fs');
const path = require('path');
const mysql = require('mysql2/promise');

const dbConfig = {host: process.env.DB_HOST || 'localhost', port: Number(process.env.DB_PORT || 3306), user: process.env.DB_USER || 'root', password: process.env.DB_PASSWORD || ''};
const database = process.env.DB_NAME || 'parttimejob_db';
const pool = mysql.createPool({...dbConfig, database, waitForConnections: true, connectionLimit: 10, dateStrings: true});
function statements(file) { return fs.readFileSync(file, 'utf8').replace(/^--.*$/gm, '').split(';').map(s => s.trim()).filter(Boolean); }
async function initializeDatabase() {
  const setup = await mysql.createConnection(dbConfig);
  await setup.query(`CREATE DATABASE IF NOT EXISTS \`${database.replace(/`/g, '``')}\``);
  await setup.end();
  const schema = path.join(__dirname, '..', 'src', 'main', 'resources', 'schema.sql');
  for (const statement of statements(schema)) await pool.query(statement);
  await pool.query(`CREATE TABLE IF NOT EXISTS complaint_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_report FOREIGN KEY (report_id) REFERENCES reports(id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_sender FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_chat_report_created (report_id, created_at)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`);
  // Migration: add location_photo_url if missing
  try { await pool.query('ALTER TABLE catering_jobs ADD COLUMN location_photo_url TEXT'); } catch (e) { if (e.errno !== 1060) throw e; }
  if (process.env.RUN_SEED !== 'false') {
    const seed = path.join(__dirname, '..', 'src', 'main', 'resources', 'data.sql');
    if (fs.existsSync(seed)) for (const statement of statements(seed)) try { await pool.query(statement); } catch (e) { if (![1062, 1451, 1452].includes(e.errno)) throw e; }
  }
}
async function transaction(work) { const connection = await pool.getConnection(); try { await connection.beginTransaction(); const result = await work(connection); await connection.commit(); return result; } catch (e) { await connection.rollback(); throw e; } finally { connection.release(); } }
module.exports = {pool, initializeDatabase, transaction};