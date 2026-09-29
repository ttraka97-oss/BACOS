// BACOS backend — Express API skeleton
// The Android app is local-first; this server adds auth + sync + AI proxy.
//
// Setup:
//   createdb bacos && psql -d bacos -f schema.sql
//   cp ../.env.example .env  (fill values)
//   npm install && npm start

const express = require('express');
const { Pool } = require('pg');

const app = express();
app.use(express.json({ limit: '5mb' }));

const pool = new Pool({
    host: process.env.PGHOST || 'localhost',
    port: process.env.PGPORT || 5432,
    database: process.env.PGDATABASE || 'bacos',
    user: process.env.PGUSER || 'postgres',
    password: process.env.PGPASSWORD,
});

// ── health ──
app.get('/health', async (_req, res) => {
    try {
        await pool.query('SELECT 1');
        res.json({ ok: true, db: 'up' });
    } catch (e) {
        res.status(500).json({ ok: false, error: e.message });
    }
});

// ── auth (register) — password hashing REQUIRED before production ──
app.post('/auth/register', async (req, res) => {
    const { email, password } = req.body;
    if (!email || !password || password.length < 8) {
        return res.status(400).json({ error: 'invalid credentials payload' });
    }
    // TODO: hash with bcrypt/argon2 — never store plain passwords
    try {
        const { rows } = await pool.query(
            'INSERT INTO users (email, password_hash) VALUES ($1, $2) RETURNING id',
            [email, 'HASH_ME:' + password]   // placeholder — replace with bcrypt.hash
        );
        res.status(201).json({ user_id: rows[0].id });
    } catch (e) {
        res.status(409).json({ error: 'email exists' });
    }
});

// ── sync: push student state from the device ──
app.post('/sync/progress', async (req, res) => {
    const { profile_id, items } = req.body; // items: [{lesson_id, mastery, status, attempts, correct}]
    if (!profile_id || !Array.isArray(items)) return res.status(400).json({ error: 'bad payload' });
    const client = await pool.connect();
    try {
        await client.query('BEGIN');
        for (const it of items) {
            await client.query(
                `INSERT INTO progress (profile_id, lesson_id, mastery, status, attempts, correct, last_studied_at)
                 VALUES ($1,$2,$3,$4,$5,$6,now())
                 ON CONFLICT (profile_id, lesson_id)
                 DO UPDATE SET mastery=$3, status=$4, attempts=$5, correct=$6, last_studied_at=now()`,
                [profile_id, it.lesson_id, it.mastery, it.status, it.attempts, it.correct]
            );
        }
        await client.query('COMMIT');
        res.json({ ok: true, synced: items.length });
    } catch (e) {
        await client.query('ROLLBACK');
        res.status(500).json({ error: e.message });
    } finally {
        client.release();
    }
});

// ── sync: pull due reviews (server-authoritative variant) ──
app.get('/sync/reviews/:profileId', async (req, res) => {
    try {
        const { rows } = await pool.query(
            `SELECT card_id, ease, interval_days, repetitions, due_at, lapses
             FROM review_states WHERE profile_id = $1 AND due_at <= now()
             ORDER BY due_at LIMIT 100`,
            [req.params.profileId]
        );
        res.json({ cards: rows });
    } catch (e) {
        res.status(500).json({ error: e.message });
    }
});

// ── AI proxy: the ONLY place an LLM key should live ──
app.post('/ai/tutor', async (req, res) => {
    const { message, lesson_context, history } = req.body;
    if (!message) return res.status(400).json({ error: 'message required' });

    // RAG guard: restrict answers to verified curriculum chunks
    const { rows } = await pool.query(
        `SELECT content FROM knowledge_chunks
         WHERE lesson_id = $1 LIMIT 8`,
        [lesson_context || null]
    );

    // Example wiring for a hosted provider (OpenAI-compatible):
    // const r = await fetch(process.env.AI_BASE_URL + '/chat/completions', {
    //     method: 'POST',
    //     headers: { 'Authorization': `Bearer ${process.env.AI_API_KEY}` },
    //     body: JSON.stringify({ model: process.env.AI_MODEL, messages: [...] })
    // });
    res.json({
        reply: `[BACOS backend] AI provider not configured. Set AI_BASE_URL and AI_API_KEY in .env. Verified context chunks found: ${rows.length}`,
        context_used: rows.length,
    });
});

const port = process.env.PORT || 8080;
app.listen(port, () => console.log(`BACOS backend on :${port}`));
