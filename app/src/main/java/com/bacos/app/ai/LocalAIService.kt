package com.bacos.app.ai

/**
 * LocalAIService — the offline BAC AI.
 *
 * NOT a chatbot parroting text: it interprets the student's request,
 * pulls REAL data from the BAC BRAIN context, and returns replies
 * with actionable next steps. Deterministic and private — nothing
 * leaves the device.
 */
class LocalAIService : AIService {

    override val name = "BAC AI (محلي)"

    override suspend fun respond(userMessage: String, mode: AIService.Mode, context: AIService.Context): AIService.AiReply {
        val msg = userMessage.trim()
        val lowered = msg.lowercase()

        return when {
            // ── Quiz requests: "اختبرني في الدوال" / "test me"
            containsAny(lowered, listOf("اختبرني", "اختبار", "test me", "quiz", "اسئلة", "أسئلة")) -> {
                val subject = guessTopic(lowered, context)
                AIService.AiReply(
                    text = "ممتاز، خلاص جهزت لك اختبار في $subject.\nالأسئلة مختارة حسب مستواك — تبدا باللي ضعيف فيها وترتفع الصعوبة.",
                    actions = listOf(AIService.AiAction("quiz:$subject", "ابدأ الاختبار", subject)),
                    followUps = listOf("بعد الاختبار حلل أخطائي", "أعطني تلميحاً قبل ما نبدا"),
                )
            }

            // ── Mistake review
            containsAny(lowered, listOf("أخطائي", "اخطائي", "أخطاء", "mistakes", "خطأي")) -> {
                if (context.openMistakes == 0) AIService.AiReply(
                    text = "ما عندك حتى خطأ مسجل حالياً — نقية 🎯\nكمل تمارين باش نكتشف نقاط ضعفك ونبنيو دفتر أخطائك.",
                    followUps = listOf("اختبرني في المشتقات", "شنو نراجع اليوم؟"),
                ) else AIService.AiReply(
                    text = "عندك ${context.openMistakes} خطأ محتاج تصليح.\nالمفهوم الأكثر تكراراً في أخطائك هو الأساسي — نصلحو خطوة بخطوة؟",
                    actions = listOf(AIService.AiAction("mistakes", "افتح دفتر الأخطاء")),
                    followUps = listOf("ارحمني اليوم", "وين نقاط قوتي؟"),
                )
            }

            // ── Plan / what to study
            containsAny(lowered, listOf("خطة", "plan", "شنو نراجع", "ماذا أراجع", "وش نراجع", "وين نبدا", "برنامج")) -> {
                val parts = mutableListOf<String>()
                parts.add("خطة اليوم من عند BAC BRAIN:")
                if (context.dueReviews > 0) parts.add("• ${context.dueReviews} بطاقة جاهزة للمراجعة (spaced repetition)")
                context.weakestSubject?.let { parts.add("• جلسة تدريب في $it — أضعف مادة عندك") }
                parts.add("• درس جديد + تمارين باش يبقى الstreak حي")
                if (context.openMistakes > 0) parts.add("• تصليح ${context.openMistakes} خطأ قديم")
                AIService.AiReply(
                    text = parts.joinToString("\n"),
                    actions = listOf(
                        AIService.AiAction("session", "ابدأ جلسة اليوم"),
                        AIService.AiAction("mistakes", "شوف أخطائي"),
                    ),
                    followUps = listOf("عندي غير ساعة اليوم", "بدل لي الخطة"),
                )
            }

            // ── Time constraint: "عندي ساعة"
            containsAny(lowered, listOf("ساعة", "دقيقة", "وقت", "time", "hour", "minute")) -> {
                AIService.AiReply(
                    text = "واضح — خطة مضغوطة 60 دقيقة:\n1) 15 دقيقة مراجعة بطاقات (الأكثر إلحاحاً)\n2) 30 دقيقة تمارين في ${context.weakestSubject ?: "أضعف مادة"}\n3) 15 دقيقة تصليح أخطاء قديمة\n\nالجودة قبل الكمية — بلا تشتيت.",
                    actions = listOf(AIService.AiAction("session", "ابدأ جلسة 60 دقيقة")),
                    followUps = listOf("بدل الترتيب", "خفف لي الخطة"),
                )
            }

            // ── "ما فهمتش" → simplify (the prompt's example)
            containsAny(lowered, listOf("ما فهمت", "ما فهمتش", "ما فهمه", "صعيب", "معقد", "ما وليت", "didn't understand", "simplify", "بسط")) -> {
                val lesson = context.activeLessonTitle ?: "الدرس الحالي"
                AIService.AiReply(
                    text = "عادي، خلينا نرجعو خطوة.\n\nنشرحلك $lesson من الصفر بأسلوب بسيط: الفكرة الأساسية → مثال صغير → القاعدة. وإذا مازال صعيب، نقسموه لجزأين.",
                    actions = listOf(AIService.AiAction("lesson:simplify", "اشرحلي من الصفر", lesson)),
                    followUps = listOf("جرب جاوبني على سؤال", "أعطني مثال محلول"),
                )
            }

            // ── Strength / progress
            containsAny(lowered, listOf("نقاط قوتي", "مستواي", "readiness", "تحضيري", "جاهز")) -> {
                val score = context.readinessScore
                val verdict = when {
                    score >= 80 -> "أنت في منطقة الأمان — ثبّت المستوى بالمراجعة المنتظمة."
                    score >= 50 -> "نصف الطريق قطعته. النقلة الجاية: تمارين أكثر في أضعف مادة."
                    else -> "بدايتك صحيحة. التركيز الآن: أساسيات كل مادة قبل التعمق."
                }
                AIService.AiReply(
                    text = "مستوى تحضيرك: $score/100\n$verdict",
                    actions = listOf(AIService.AiAction("progress", "شوف تحليلي الكامل")),
                    followUps = listOf("كيف نرفع المستوى بسرعة؟", "اختبرني باش تتأكد"),
                )
            }

            // ── Hint request
            containsAny(lowered, listOf("تلميح", "hint", "عاوني", "ساعدني")) -> {
                AIService.AiReply(
                    text = "ما نعطيك الحل الكامل دغيا 😄\nخلاص — تلميح: ركز على المعطيات المباشرة في السؤال، وابدا من القاعدة الأساسية للمفهوم.\nإذا مازال صعيب، نزيدك تلميح ثاني ولا نحلوه مع بعض خطوة بخطوة.",
                    followUps = listOf("زيدني تلميح", "حل معايا خطوة بخطوة", "الحل كامل"),
                )
            }

            // ── Default: explain-style tutor flow on current lesson
            else -> {
                val lesson = context.activeLessonTitle
                val body = if (lesson != null) {
                    "الموضوع اليوم: $lesson.\nنقدر نشرحلك بثلاث طرق: مبسطة، متقدمة، ولا بطريقة الباك. قولي شتسلى."
                } else {
                    "أنا BAC AI — نعرف مستواك ونوجهك.\nتقدر تسألني: \"اشرحلي المشتقات\"، \"اختبرني في الفيزياء\"، \"شنو نراجع اليوم؟\"، \"حلل أخطائي\"."
                }
                AIService.AiReply(
                    text = body,
                    followUps = listOf("اختبرني في ${context.weakestSubject ?: "الرياضيات"}", "شنو نراجع اليوم؟", "بسط لي هذا الدرس"),
                )
            }
        }
    }

    private fun containsAny(text: String, keys: List<String>): Boolean = keys.any { text.contains(it) }

    private fun guessTopic(lowered: String, context: AIService.Context): String {
        return when {
            lowered.contains("فيز") || lowered.contains("phys") -> "الفيزياء"
            lowered.contains("كيم") || lowered.contains("chem") -> "الكيمياء"
            lowered.contains("حي") || lowered.contains("علوم") || lowered.contains("svt") -> "علوم الطبيعة"
            lowered.contains("رياض") || lowered.contains("math") -> "الرياضيات"
            else -> context.weakestSubject ?: "الرياضيات"
        }
    }
}
