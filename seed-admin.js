const bcrypt = require("bcryptjs");
const Database = require("better-sqlite3");
const db = new Database(process.env.DB_FILE || "./jovan-support.db");
db.exec(`CREATE TABLE IF NOT EXISTS users (
 id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, email TEXT UNIQUE NOT NULL,
 phone TEXT UNIQUE, password_hash TEXT NOT NULL, role TEXT NOT NULL DEFAULT 'user',
 verification_status TEXT NOT NULL DEFAULT 'pending', created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
)`);
const email = process.env.ADMIN_EMAIL;
const password = process.env.ADMIN_PASSWORD;
if (!email || !password) {
  console.error("Set ADMIN_EMAIL and ADMIN_PASSWORD.");
  process.exit(1);
}
const hash = bcrypt.hashSync(password, 12);
try {
  db.prepare(`INSERT INTO users(name,email,password_hash,role,verification_status) VALUES(?,?,?,?,?)`)
    .run("Jovan Support Center Admin", email.toLowerCase(), hash, "admin", "verified");
  console.log("Admin created:", email);
} catch {
  console.error("Could not create admin. The email may already exist.");
}
