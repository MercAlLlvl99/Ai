# RED QUEEN AI — Android starter project

A cinematic, red-themed Android voice assistant inspired by the *style* of a sci-fi security AI. This is an independent fan-inspired project; it does not use Resident Evil footage, character assets, or an imitation of the film actor's voice.

## Included

- Android app shell with a red holographic-style animated interface
- Voice input through Android `SpeechRecognizer` (English UK by default)
- Spoken answers through Android Text-to-Speech (English UK by default; uses the device's installed voice)
- Chat interface and recent conversation history stored locally in the app's WebView storage
- Optional OpenAI-compatible Chat Completions endpoint; API key is stored in the app's private SharedPreferences
- Keyboard input when speech recognition is unavailable

## Important limitations

This workspace does not contain the Android SDK, Gradle installation, or signing credentials, so a compiled APK could not be produced or tested here. The ZIP contains the complete Android Studio project source. The AI conversation feature requires an API key and internet connection. The speech features use the Android speech/TTS services installed on the phone; the exact voice varies by device and language pack. This is not a movie-accurate 3D character or a real Umbrella/Resident Evil product.

## Build the APK

1. Install **Android Studio** on a computer and let its setup wizard install Android SDK Platform 35 and the Android SDK Build-Tools.
2. Unzip this folder and open the `RedQueenAI` folder in Android Studio.
3. Allow Gradle sync to complete. Android Studio will download the Android Gradle Plugin and Kotlin plugin if needed.
4. Select **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
5. The debug APK will be created at `app/build/outputs/apk/debug/app-debug.apk`.
6. Transfer that APK to your Android phone, open it, and allow installation from that source when Android asks.
7. Open RED QUEEN AI → ⚙ Settings. Enter a Chat Completions URL, model, and API key. For OpenAI, the default URL is `https://api.openai.com/v1/chat/completions`; enter a model available to your account. Keep your API key private.
8. Tap the microphone and grant microphone permission. You can also type messages.

## Build from a terminal

With Android SDK and Java 17+ installed, from the project directory run:

```bash
gradle assembleDebug
```

Or use Android Studio's bundled Gradle after opening the project. If your local environment requires a Gradle wrapper, create it with the Gradle version supported by the Android Gradle Plugin, then run `./gradlew assembleDebug`.

## Privacy notes

- Speech recognition may be processed by the speech service configured on the Android device.
- Messages sent to the configured AI endpoint and the API key are transmitted to that provider.
- The app does not include a bundled API key. Never put a private API key into a public source repository.
- Conversation history is held in the app's local WebView storage. Clearing app data removes it.
