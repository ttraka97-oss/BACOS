package com.bacos.app.ai

/**
 * ═══════════════════════════════════════════════════════════
 *  AI ABSTRACTION LAYER
 * ═══════════════════════════════════════════════════════════
 *
 *  The app NEVER talks to an LLM provider directly from the UI.
 *  Everything goes through [AIService], so the provider can be
 *  swapped (local rules → remote OpenAI/Gemini/custom gateway)
 *  without touching a single screen.
 *
 *  Currently active implementation: [LocalAIService]
 *  — a deterministic, offline tutor driven by the BAC BRAIN data
 *    (curriculum, progress, mistakes). It performs REAL actions
 *    (builds quizzes, lists mistakes, generates plans).
 *
 *  To plug a hosted LLM: implement [AIService] with your API
 *    (see docs/AI_SETUP.md) and register it in [AiProvider].
 * ═══════════════════════════════════════════════════════════
 */
interface AIService {
    val name: String

    /** Modes of teaching — the student picks how the AI behaves. */
    enum class Mode { EXPLAIN, TEST_ME, HINT, SIMPLIFY, DEEP_DIVE, EXAM_MODE }

    data class AiAction(
        val key: String,          // "quiz:subjectId" | "review" | "lesson:lessonId" ...
        val label: String,        // button label
        val payload: String = "",
    )

    data class AiReply(
        val text: String,
        val actions: List<AiAction> = emptyList(),
        val followUps: List<String> = emptyList(),
    )

    /** Context injected from the app state — never hallucinated. */
    data class Context(
        val activeLessonTitle: String? = null,
        val dueReviews: Int = 0,
        val openMistakes: Int = 0,
        val weakestSubject: String? = null,
        val readinessScore: Int = 0,
    )

    suspend fun respond(userMessage: String, mode: Mode, context: Context): AiReply
}

/** Simple provider registry. */
object AiProvider {
    @Volatile private var service: AIService = LocalAIService()

    fun get(): AIService = service

    fun install(custom: AIService) {
        service = custom
    }
}
