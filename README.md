# BACOS — نظام الباك الذكي

**Personal AI Study System** لتلاميذ البكالوريا الجزائرية.
ليس "تطبيق دروس" — بل نظام يعرف الطالب: مستواه، تقدمه، نقاط ضعفه، والدرس الذي نسيه، ويقرر معه ماذا يفعل **الآن**.

## ما هو حقيقي في هذا التطبيق

| النظام | الحالة | الوصف |
|---|---|---|
| Spaced Repetition | ✅ حقيقي | خوارزمية SM-2 كاملة: ease/interval/repetitions/lapses — جدول مراجعة تكيفي لكل بطاقة |
| BAC READINESS™ | ✅ حقيقي | صيغة شفافة: 60% إتقان + 25% تغطية + 15% استرجاع + 10% انتظام |
| Adaptive Quiz | ✅ حقيقي | اختيار الأسئلة موزون على نقاط ضعفك + تعديل الصعوبة حسب دقتك الأخيرة |
| دفتر الأخطاء | ✅ حقيقي | كل خطأ يُسجل تلقائياً مع المفهوم والشرح وزر "تدرب على هذا الخطأ" |
| BAC BRAIN (المخطط اليومي) | ✅ حقيقي | يبني مهمة اليوم من: البطاقات المستحقة + الدرس التالي + الأخطاء المفتوحة + أضعف مادة |
| قاعدة البيانات | ✅ حقيقي | Room (SQLite) — 10 جداول مع Foreign Keys |
| المحتوى | ✅ حقيقي | منهج 3AS علوم تجريبية: رياضيات، فيزياء-كيمياء، SVT بأسئلة وشرح حقيقي |
| محرّك AI | ⚙️ معماري | طبقة تجريد `AIService` + تنفيذ محلي offline يفهم الطلب وينفذ أفعال حقيقية. ربط LLM خارجي = docs/AI_SETUP.md |
| Backend | ⚠️ كود جاهز | `backend/` يحتوي schema.sql (PostgreSQL) + Express API. التطبيق local-first ويعمل 100% بدون سيرفر |

## التشغيل

```bash
git clone https://github.com/ttraka97-oss/BACOS.git
cd BACOS
./gradlew assembleDebug          # يبني APK
# النتيجة: app/build/outputs/apk/debug/app-debug.apk
```

## بناء APK (خطوة بخطوة)

1. ثبّت **JDK 17**
2. `chmod +x gradlew`
3. `./gradlew assembleDebug`
4. `adb install app/build/outputs/apk/debug/app-debug.apk` (أو انسخ الـAPK للهاتف)

للـ release الموقّع:
```bash
keytool -genkey -v -keystore bacos.keystore -alias bacos -keyalg RSA -validity 10000
./gradlew assembleRelease
```

## الاختبارات

```bash
./gradlew testDebugUnitTest
```
تغطي: SM-2 (الفترات، الانهيار، حدود ease)، محرك Readiness (الصيغة كاملة، حدود 0-100).

## البنية المعمارية

```
app/src/main/java/com/bacos/app/
├── ai/            ← AI abstraction layer (AIService + LocalAIService)
├── data/
│   ├── db/        ← Room: entities, DAOs, database (10 جداول)
│   └── seed/      ← منهج الباك + بيانات التجربة
├── domain/        ← المحركات النقية: SRS, Readiness, Quiz, Planner
└── ui/
    ├── theme/     ← Design system: ألوان، IBM Plex Sans Arabic، Spacing tokens
    ├── components/← مكونات قابلة لإعادة الاستخدام
    └── screens/   ← Onboarding, Home, Learn, Lesson, Practice, Quiz, Review, Mistakes, AI, Profile
```

## AI Layer

التطبيق لا يتصل بأي LLM مباشرة. كل الطلبات تمر عبر `AIService`:

```kotlin
interface AIService {
    suspend fun respond(userMessage: String, mode: Mode, context: Context): AiReply
}
// لربط مزود خارجي:
AiProvider.install(MyRemoteAIService(apiKey))
```
انظر `docs/AI_SETUP.md`.

## المتغيرات البيئية (للـ backend)

انظر `.env.example` — لا تضع أي مفتاح حقيقي في GitHub.

## الخصوصية

التطبيق يعمل offline بالكامل. لا تخرج أي بيانات من الجهاز في الوضع الافتراضي.
