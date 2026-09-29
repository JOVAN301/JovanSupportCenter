const express = require("express");
const cors = require("cors");
const bcrypt = require("bcryptjs");
const jwt = require("jsonwebtoken");
const Database = require("better-sqlite3");
require("dotenv").config?.();

const app = express();
app.use(cors({ origin: true }));
app.use(express.json({ limit: "256kb" }));

const PORT = process.env.PORT || 3000;
const JWT_SECRET = process.env.JWT_SECRET || "CHANGE_ME_IN_PRODUCTION";
const db = new Database(process.env.DB_FILE || "./jovan-support.db");

db.exec(`
PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS users (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  email TEXT UNIQUE NOT NULL,
  phone TEXT UNIQUE,
  password_hash TEXT NOT NULL,
  role TEXT NOT NULL DEFAULT 'user' CHECK(role IN ('user','admin')),
  verification_status TEXT NOT NULL DEFAULT 'pending'
    CHECK(verification_status IN ('pending','verified','rejected')),
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS requests (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER NOT NULL,
  category TEXT NOT NULL,
  title TEXT NOT NULL,
  description TEXT NOT NULL,
  amount_needed INTEGER,
  location TEXT,
  status TEXT NOT NULL DEFAULT 'pending'
    CHECK(status IN ('pending','verified','rejected','closed')),
  verified_by INTEGER,
  verified_at TEXT,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY(user_id) REFERENCES users(id),
  FOREIGN KEY(verified_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS contributions (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  request_id INTEGER NOT NULL,
  supporter_id INTEGER NOT NULL,
  amount INTEGER NOT NULL CHECK(amount > 0),
  payment_status TEXT NOT NULL DEFAULT 'pending'
    CHECK(payment_status IN ('pending','successful','failed','refunded')),
  provider TEXT,
  provider_reference TEXT,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY(request_id) REFERENCES requests(id),
  FOREIGN KEY(supporter_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS notifications (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  user_id INTEGER NOT NULL,
  request_id INTEGER,
  type TEXT NOT NULL,
  message TEXT NOT NULL,
  read_at TEXT,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY(user_id) REFERENCES users(id),
  FOREIGN KEY(request_id) REFERENCES requests(id)
);

CREATE TABLE IF NOT EXISTS audit_logs (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  actor_id INTEGER,
  action TEXT NOT NULL,
  entity_type TEXT NOT NULL,
  entity_id INTEGER,
  details TEXT,
  created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY(actor_id) REFERENCES users(id)
);
`);

function auth(req, res, next) {
  const header = req.headers.authorization || "";
  const token = header.startsWith("Bearer ") ? header.slice(7) : null;
  if (!token) return res.status(401).json({error:"Authentication required"});
  try {
    req.user = jwt.verify(token, JWT_SECRET);
    next();
  } catch {
    res.status(401).json({error:"Invalid or expired token"});
  }
}

function admin(req, res, next) {
  if (req.user.role !== "admin") return res.status(403).json({error:"Admin access required"});
  next();
}

function log(actorId, action, entityType, entityId, details="") {
  db.prepare(`INSERT INTO audit_logs(actor_id,action,entity_type,entity_id,details)
              VALUES(?,?,?,?,?)`).run(actorId, action, entityType, entityId, details);
}

app.get("/health", (_, res) => res.json({ok:true, service:"Jovan Support Center"}));

app.post("/auth/register", async (req, res) => {
  const {name,email,phone,password} = req.body;
  if (!name || !email || !password) return res.status(400).json({error:"Name, email and password are required"});
  if (password.length < 8) return res.status(400).json({error:"Password must be at least 8 characters"});
  try {
    const hash = await bcrypt.hash(password, 12);
    const result = db.prepare(`INSERT INTO users(name,email,phone,password_hash) VALUES(?,?,?,?)`)
      .run(name.trim(), email.trim().toLowerCase(), phone || null, hash);
    const user = {id:Number(result.lastInsertRowid), name, email, role:"user", verification_status:"pending"};
    const token = jwt.sign(user, JWT_SECRET, {expiresIn:"7d"});
    res.status(201).json({user, token});
  } catch (e) {
    res.status(409).json({error:"Email or phone may already be registered"});
  }
});

app.post("/auth/login", async (req, res) => {
  const {email,password} = req.body;
  const user = db.prepare(`SELECT * FROM users WHERE email=?`).get((email||"").toLowerCase());
  if (!user || !(await bcrypt.compare(password || "", user.password_hash)))
    return res.status(401).json({error:"Invalid email or password"});
  const safe = {id:user.id,name:user.name,email:user.email,phone:user.phone,role:user.role,verification_status:user.verification_status};
  res.json({user:safe, token:jwt.sign(safe, JWT_SECRET,{expiresIn:"7d"})});
});

app.get("/me", auth, (req,res) => {
  const user = db.prepare(`SELECT id,name,email,phone,role,verification_status,created_at FROM users WHERE id=?`).get(req.user.id);
  res.json(user);
});

app.post("/requests", auth, (req,res) => {
  const {category,title,description,amount_needed,location} = req.body;
  if (!category || !title || !description) return res.status(400).json({error:"Category, title and description are required"});
  const r = db.prepare(`INSERT INTO requests(user_id,category,title,description,amount_needed,location)
                        VALUES(?,?,?,?,?,?)`)
    .run(req.user.id,category,title,description,amount_needed || null,location || null);
  log(req.user.id,"request_submitted","request",r.lastInsertRowid);
  res.status(201).json({id:Number(r.lastInsertRowid),status:"pending"});
});

app.get("/requests/public", (req,res) => {
  const rows = db.prepare(`
    SELECT r.id,r.category,r.title,r.description,r.amount_needed,r.location,r.created_at,
           u.name AS requester
    FROM requests r JOIN users u ON u.id=r.user_id
    WHERE r.status='verified'
    ORDER BY r.created_at DESC
  `).all();
  res.json(rows);
});

app.get("/requests/mine", auth, (req,res) => {
  res.json(db.prepare(`SELECT * FROM requests WHERE user_id=? ORDER BY created_at DESC`).all(req.user.id));
});

app.get("/admin/users", auth, admin, (req,res) => {
  res.json(db.prepare(`
    SELECT id,name,email,phone,role,verification_status,created_at
    FROM users ORDER BY created_at DESC
  `).all());
});

app.post("/admin/users/:id/verify", auth, admin, (req,res) => {
  const id = Number(req.params.id);
  const user = db.prepare(`SELECT * FROM users WHERE id=?`).get(id);
  if (!user) return res.status(404).json({error:"User not found"});
  db.prepare(`UPDATE users SET verification_status='verified' WHERE id=?`).run(id);
  db.prepare(`INSERT INTO notifications(user_id,type,message) VALUES(?,?,?)`)
    .run(id,"user_verified","Your Jovan Support Center account has been verified.");
  log(req.user.id,"user_verified","user",id);
  res.json({ok:true,status:"verified"});
});

app.post("/admin/users/:id/reject", auth, admin, (req,res) => {
  const id = Number(req.params.id);
  const user = db.prepare(`SELECT * FROM users WHERE id=?`).get(id);
  if (!user) return res.status(404).json({error:"User not found"});
  db.prepare(`UPDATE users SET verification_status='rejected' WHERE id=?`).run(id);
  db.prepare(`INSERT INTO notifications(user_id,type,message) VALUES(?,?,?)`)
    .run(id,"user_rejected","Your account verification was not approved. Please contact support.");
  log(req.user.id,"user_rejected","user",id);
  res.json({ok:true,status:"rejected"});
});

app.get("/admin/requests", auth, admin, (req,res) => {
  res.json(db.prepare(`
    SELECT r.*,u.name requester_name,u.email requester_email,u.phone requester_phone
    FROM requests r JOIN users u ON u.id=r.user_id
    ORDER BY CASE r.status WHEN 'pending' THEN 0 ELSE 1 END, r.created_at DESC
  `).all());
});

app.post("/admin/requests/:id/verify", auth, admin, (req,res) => {
  const id = Number(req.params.id);
  const r = db.prepare(`SELECT * FROM requests WHERE id=?`).get(id);
  if (!r) return res.status(404).json({error:"Request not found"});
  db.prepare(`UPDATE requests SET status='verified',verified_by=?,verified_at=CURRENT_TIMESTAMP WHERE id=?`)
    .run(req.user.id,id);
  db.prepare(`INSERT INTO notifications(user_id,request_id,type,message) VALUES(?,?,?,?)`)
    .run(r.user_id,id,"request_verified","Your support request has been verified and is now public.");
  log(req.user.id,"request_verified","request",id);
  res.json({ok:true,status:"verified"});
});

app.post("/admin/requests/:id/reject", auth, admin, (req,res) => {
  const id = Number(req.params.id);
  const r = db.prepare(`SELECT * FROM requests WHERE id=?`).get(id);
  if (!r) return res.status(404).json({error:"Request not found"});
  db.prepare(`UPDATE requests SET status='rejected',verified_by=?,verified_at=CURRENT_TIMESTAMP WHERE id=?`)
    .run(req.user.id,id);
  db.prepare(`INSERT INTO notifications(user_id,request_id,type,message) VALUES(?,?,?,?)`)
    .run(r.user_id,id,"request_rejected","Your support request was not approved. Please contact support for details.");
  log(req.user.id,"request_rejected","request",id);
  res.json({ok:true,status:"rejected"});
});

app.post("/contributions", auth, (req,res) => {
  const {request_id,amount,provider} = req.body;
  const r = db.prepare(`SELECT * FROM requests WHERE id=? AND status='verified'`).get(request_id);
  if (!r) return res.status(404).json({error:"Verified request not found"});
  if (!amount || Number(amount) <= 0) return res.status(400).json({error:"Valid amount required"});
  const c = db.prepare(`INSERT INTO contributions(request_id,supporter_id,amount,provider)
                        VALUES(?,?,?,?)`)
    .run(request_id,req.user.id,Number(amount),provider || "manual");
  db.prepare(`INSERT INTO notifications(user_id,request_id,type,message) VALUES(?,?,?,?)`)
    .run(r.user_id,request_id,"contribution_pending","Someone has started a support contribution for your request.");
  log(req.user.id,"contribution_created","contribution",c.lastInsertRowid,`request=${request_id};amount=${amount}`);
  res.status(201).json({id:Number(c.lastInsertRowid),payment_status:"pending"});
});

app.get("/contributions/mine", auth, (req,res) => {
  res.json(db.prepare(`
    SELECT c.*,r.title FROM contributions c JOIN requests r ON r.id=c.request_id
    WHERE c.supporter_id=? ORDER BY c.created_at DESC
  `).all(req.user.id));
});

app.get("/notifications", auth, (req,res) => {
  res.json(db.prepare(`SELECT * FROM notifications WHERE user_id=? ORDER BY created_at DESC`).all(req.user.id));
});

app.post("/notifications/:id/read", auth, (req,res) => {
  db.prepare(`UPDATE notifications SET read_at=CURRENT_TIMESTAMP WHERE id=? AND user_id=?`)
    .run(Number(req.params.id),req.user.id);
  res.json({ok:true});
});

app.get("/admin/transactions", auth, admin, (req,res) => {
  res.json(db.prepare(`
    SELECT c.*,r.title,s.name supporter_name
    FROM contributions c
    JOIN requests r ON r.id=c.request_id
    JOIN users s ON s.id=c.supporter_id
    ORDER BY c.created_at DESC
  `).all());
});

app.listen(PORT, () => console.log(`Jovan Support Center API running on port ${PORT}`));
