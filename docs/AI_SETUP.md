# AI Setup — ربط BAC AI بمزوّد حقيقي

التطبيق يعمل offline مع `LocalAIService` (يفهم الطلب وينفذ أفعالاً حقيقية:
يبني اختبارات، يسرد أخطاءك، يولّد خطة اليوم).

## لربط LLM خارجي (OpenAI / Gemini / أي بوابة متوافقة)

### 1. لا تضع المفتاح داخل الـ APK أبداً
مفاتيح أي مزود LLM توضع في السيرفر (backend). الـ APK يكلّم الـ backend فقط.

### 2. فعّل الوكيل في backend/server.js
```js
const r = await fetch(process.env.AI_BASE_URL + '/chat/completions', {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${process.env.AI_API_KEY}` },
    body: JSON.stringify({
        model: process.env.AI_MODEL,
        messages: [
            { role: 'system', content: 'أنت مساعد باك جزائري. جاوب حسب المحتوى المعتمد فقط.' },
            ...history,
            { role: 'user', content: message }
        ]
    })
});
```
RAG: جدول `knowledge_chunks` يخزن المحتوى المعتمد — مرّر مقاطع الدرس
مع الطلب كي لا يخترع الـ LLM منهجاً.

### 3. نفّذ RemoteAIService في التطبيق
```kotlin
class RemoteAIService(private val baseUrl: String) : AIService {
    override val name = "BAC AI (سحابي)"
    override suspend fun respond(userMessage: String, mode: AIService.Mode, context: AIService.Context): AIService.AiReply {
        val body = """{"message": "$userMessage", "lesson_context": ...}"""
        val resp = java.net.URL("$baseUrl/ai/tutor")
            .openConnection() as java.net.HttpURLConnection
        // ... POST + parse ...
        return AIService.AiReply(text = parsedText)
    }
}

// ثم سجّله مرة واحدة عند الإقلاع:
AiProvider.install(RemoteAIService("https://your-server.com"))
```
الشاشات لا تتغير إطلاقاً — كلها تمرّ عبر `AiProvider.get()`.

### 4. حدود معروفة
- الصور/PDF (اسأل بصورة تمرين): يتطلب نموذجاً متعدد الوسائط — نفس الواجهة
  `AIService` تستوعبها لاحقاً بإضافة `respondWithImage`.
- بلا إنترنت يعود التطبيق تلقائياً لـ LocalAIService (موصى به في التطبيق).
