-- ═══════════════════════════════════════════════════════
-- BACOS — PostgreSQL schema (backend)
-- Mirrors the Room schema used by the Android app.
-- Run: psql -U postgres -f schema.sql
-- ═══════════════════════════════════════════════════════

CREATE EXTENSION IF NOT EXISTS "pgcrypto"; -- gen_random_uuid

-- ── Users & auth ──
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,           -- bcrypt/argon2 — NEVER plain
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name TEXT NOT NULL DEFAULT '',
    branch TEXT NOT NULL DEFAULT 'sciences',
    exam_date TIMESTAMPTZ,
    daily_minutes INT NOT NULL DEFAULT 60,
    streak INT NOT NULL DEFAULT 0,
    last_active_day DATE,
    total_xp BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_profiles_user ON profiles(user_id);

-- ── Curriculum ──
CREATE TABLE IF NOT EXISTS subjects (
    id BIGSERIAL PRIMARY KEY,
    code TEXT UNIQUE NOT NULL,
    name_ar TEXT NOT NULL,
    name_fr TEXT NOT NULL,
    color_hex TEXT NOT NULL,
    order_idx INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS chapters (
    id BIGSERIAL PRIMARY KEY,
    subject_id BIGINT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    name_ar TEXT NOT NULL,
    name_fr TEXT NOT NULL,
    order_idx INT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_chapters_subject ON chapters(subject_id);

CREATE TABLE IF NOT EXISTS lessons (
    id BIGSERIAL PRIMARY KEY,
    chapter_id BIGINT NOT NULL REFERENCES chapters(id) ON DELETE CASCADE,
    title_ar TEXT NOT NULL,
    summary TEXT NOT NULL DEFAULT '',
    difficulty INT NOT NULL DEFAULT 2,
    minutes INT NOT NULL DEFAULT 30,
    sections_json JSONB NOT NULL DEFAULT '[]'
);
CREATE INDEX IF NOT EXISTS idx_lessons_chapter ON lessons(chapter_id);

CREATE TABLE IF NOT EXISTS questions (
    id BIGSERIAL PRIMARY KEY,
    lesson_id BIGINT NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    text TEXT NOT NULL,
    choices JSONB NOT NULL,
    correct_index INT NOT NULL,
    difficulty INT NOT NULL DEFAULT 2,
    explanation TEXT NOT NULL DEFAULT '',
    concept TEXT NOT NULL DEFAULT ''
);
CREATE INDEX IF NOT EXISTS idx_questions_lesson ON questions(lesson_id);

CREATE TABLE IF NOT EXISTS flashcards (
    id BIGSERIAL PRIMARY KEY,
    lesson_id BIGINT NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    front TEXT NOT NULL,
    back TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_flashcards_lesson ON flashcards(lesson_id);

-- ── Student state (sync from device) ──
CREATE TABLE IF NOT EXISTS progress (
    profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    lesson_id BIGINT NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    mastery INT NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'new',
    last_studied_at TIMESTAMPTZ,
    attempts INT NOT NULL DEFAULT 0,
    correct INT NOT NULL DEFAULT 0,
    PRIMARY KEY (profile_id, lesson_id)
);

CREATE TABLE IF NOT EXISTS review_states (
    profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    card_id BIGINT NOT NULL REFERENCES flashcards(id) ON DELETE CASCADE,
    ease REAL NOT NULL DEFAULT 2.5,
    interval_days INT NOT NULL DEFAULT 0,
    repetitions INT NOT NULL DEFAULT 0,
    due_at TIMESTAMPTZ NOT NULL,
    last_reviewed_at TIMESTAMPTZ,
    lapses INT NOT NULL DEFAULT 0,
    PRIMARY KEY (profile_id, card_id)
);
CREATE INDEX IF NOT EXISTS idx_review_due ON review_states(profile_id, due_at);

CREATE TABLE IF NOT EXISTS mistakes (
    id BIGSERIAL PRIMARY KEY,
    profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    question_id BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    concept TEXT NOT NULL DEFAULT '',
    chosen_index INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved BOOLEAN NOT NULL DEFAULT false
);
CREATE INDEX IF NOT EXISTS idx_mistakes_profile ON mistakes(profile_id, resolved);

CREATE TABLE IF NOT EXISTS study_sessions (
    id BIGSERIAL PRIMARY KEY,
    profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    day DATE NOT NULL,
    minutes INT NOT NULL DEFAULT 0,
    type TEXT NOT NULL,
    xp INT NOT NULL DEFAULT 0,
    questions INT NOT NULL DEFAULT 0,
    correct INT NOT NULL DEFAULT 0,
    started_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_sessions_profile_day ON study_sessions(profile_id, day);

-- ── AI conversations (for RAG-tutored history) ──
CREATE TABLE IF NOT EXISTS ai_conversations (
    id BIGSERIAL PRIMARY KEY,
    profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    lesson_id BIGINT REFERENCES lessons(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS ai_messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES ai_conversations(id) ON DELETE CASCADE,
    role TEXT NOT NULL CHECK (role IN ('user', 'assistant')),
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_ai_messages_conv ON ai_messages(conversation_id);

-- ── Gamification ──
CREATE TABLE IF NOT EXISTS achievements (
    id BIGSERIAL PRIMARY KEY,
    profile_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    key TEXT NOT NULL,
    unlocked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (profile_id, key)
);

-- ── AI layer: verified curriculum for RAG ──
-- (pgvector recommended for embeddings in production)
-- CREATE EXTENSION IF NOT EXISTS vector;
CREATE TABLE IF NOT EXISTS knowledge_chunks (
    id BIGSERIAL PRIMARY KEY,
    lesson_id BIGINT REFERENCES lessons(id) ON DELETE CASCADE,
    content TEXT NOT NULL,                 -- verified lesson content (no hallucinated curriculum)
    kind TEXT NOT NULL DEFAULT 'lesson'    -- lesson | bac_exam | explanation
);
CREATE INDEX IF NOT EXISTS idx_knowledge_lesson ON knowledge_chunks(lesson_id);
