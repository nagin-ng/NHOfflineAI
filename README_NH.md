# NH Offline AI — Android Studio Project

Bina internet ke, kisi bhi GGUF AI model ko Android mobile mein import karke chalane wala app.
NH brand theme: dark background + red/gold, Bebas Neue display font, card-based layout.

---

## ⚠️ Zaroori baat — CoGo (Code on the Go) is *not* enough for this project

Tumhare pichle NH projects (NH Vault, NHBuilder, JarvisAI, etc.) pure Kotlin the, isliye CoGo pe
build ho jaate the. **Ye project alag hai** — isme asli AI inference chalane ke liye llama.cpp ka
C++ engine hai jo **native code (NDK + CMake)** se compile hota hai.

CoGo sirf normal Gradle/Kotlin build karta hai — NDK toolchain (C++ compiler, ~1-2GB) download
aur native `.so` build karna mobile pe practically possible nahi hai.

**Isliye pehli baar build karne ke liye Android Studio (PC/laptop) ya GitHub Actions (cloud, free)
chahiye hoga — sirf ek baar.** Uske baad jo `.apk` banega usse tum test kar sakte ho, aur agar
future mein sirf UI/Kotlin side ke chhote changes karne hain (naye button, naya screen, chat UI
tweak), wo CoGo pe normal ho jaayenge kyunki native part already built rahega cache mein.

Agar tumhare paas PC access bilkul nahi hai, best option: is project ko GitHub pe push karo aur
GitHub Actions se free cloud build karwao (main isme bhi help kar sakta hoon agar chahiye).

---

## Project structure

```
NHOfflineAI/
├── app/            → NH UI (MainActivity, chat screen, dark/red/gold theme)
├── lib/             → Kotlin wrapper + JNI bridge (nhdeveloper.offlineai.engine)
│   └── src/main/cpp/  → ai_chat.cpp (JNI) + CMakeLists.txt
└── llama-core/      → llama.cpp ka core engine (ggml, src, common) — offline inference yahin se chalta hai
```

- Package namespace: `nhdeveloper.offlineai`
- Engine namespace: `nhdeveloper.offlineai.engine`
- minSdk 33 (Android 13+) — llama.cpp ke naye ggml backend ke liye zaroori hai

## Kaise chalega (user flow)

1. App kholo → dark NH screen dikhega, top pe "NH OFFLINE AI" gold text + red-gold divider line.
2. Neeche folder icon (red FAB) dabao → apna `.gguf` model file pick karo (phone storage se).
3. App model ko parse karke apne private storage mein copy karega, phir load karega.
4. Model ready hote hi input box enable ho jaayega, FAB send icon mein badal jaayega.
5. Message likho aur bhejo — jawab card-style bubbles mein stream hoga (red = tumhara message,
   dark-gold-bordered card = AI ka jawab).

## Offline model kahan se milega

GGUF format model chahiye (quantized, chhota size — mobile ke liye Q4_K_M jaise variants best
hain). Ye Hugging Face pe free milte hain, e.g. TinyLlama, Qwen2.5-0.5B/1.5B-Instruct-GGUF,
Phi-3-mini-GGUF. 1-2GB tak ke model phone pe smoothly chal jaate hain. Model ek baar download
karke phone storage mein rakho, phir app se import karo — uske baad app poori tarah offline
kaam karta hai.

## NH theme kahan define hai

- `app/src/main/res/values/colors.xml` — sab NH red/gold/dark colors
- `app/src/main/res/values/themes.xml` — dark Material3 theme
- `app/src/main/res/font/nh_display.xml` — Bebas Neue font family
- `app/src/main/res/layout/activity_main.xml` — card-based chat screen
- `app/src/main/res/layout/item_message_user.xml` / `item_message_assistant.xml` — chat bubbles
- `app/src/main/res/drawable/ic_launcher_*.xml` — dark red/gold NH monogram launcher icon

## Native build — sirf PC pe NAHI, poori tarah phone pe bhi nahi

Hum log ne CoGo pe kaafi try kiya, aur final result clear hai: **standard Android NDK ke andar
sirf Windows/Mac/Linux-PC ke liye compiler hote hain — phone (Android host) ke liye khud koi
compiler include nahi hota.** Isiliye `llama.cpp` jaisa native C++ engine seedhe phone pe compile
karna kabhi possible nahi hoga, chahe Gradle config kitna bhi sahi kyun na ho. Ye ek platform-level
limitation hai, humare code ka issue nahi.

### Fix: GitHub Actions se free cloud build (PC ki zaroorat nahi)

Is project mein `.github/workflows/build.yml` already daala hua hai jo real Linux server pe
(GitHub ke free cloud runner) automatically APK build karta hai — wahan asli PC-jaisa NDK/CMake
available hota hai, isliye native build bina kisi problem ke ho jaata hai.

**Steps (sab phone se, browser ya GitHub app se ho sakta hai):**

1. GitHub.com pe account banao (agar nahi hai) aur ek naya **empty** repository banao
   (e.g. `NHOfflineAI`) — Private ya Public, jo chaho.
2. Is poore project folder (`NHOfflineAI/`) ko us repo mein upload karo. Sabse aasan tareeka:
   GitHub website pe repo kholo → "Add file" → "Upload files" → poora folder drag karke daal do
   (ya `.zip` ko GitHub Desktop/web se extract karke upload karo).
3. Upload ho jaane ke baad, repo ke **"Actions"** tab pe jao.
4. "Build NH Offline AI APK" workflow dikhega — usse ek baar **manually run karo**
   ("Run workflow" button) — ya bas naya push karne pe wo apne aap chal jaayega.
5. 5-10 minute mein build complete ho jaayega (hara tick ✅). Us run ke andar
   **"Artifacts"** section mein `NHOfflineAI-debug-apk` milega — wahan se `.apk` download karo.
6. Wahi `.apk` file phone pe install karke test karo.

Iske baad jab bhi tum UI/Kotlin side ke chhote changes karte ho (naya button, chat screen tweak,
colors, strings), wahi changes GitHub pe push karte rehna — Actions khud naya APK bana dega.
CoGo abhi bhi kaam aayega agar tumhe sirf `.kt`/`.xml` files edit karke local preview dekhna ho,
bas final **native build wala APK** GitHub Actions se hi banega.


