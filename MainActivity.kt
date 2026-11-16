package com.redqueen.ai

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var web: WebView
    private var tts: TextToSpeech? = null
    private var speech: SpeechRecognizer? = null
    private val worker = Executors.newSingleThreadExecutor()
    private val prefs by lazy { getSharedPreferences("redqueen", MODE_PRIVATE) }
    private val micRequest = 41

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(5, 4, 8)
        window.navigationBarColor = android.graphics.Color.rgb(5, 4, 8)
        tts = TextToSpeech(this, this)
        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.webChromeClient = WebChromeClient()
        web.webViewClient = WebViewClient()
        web.addJavascriptInterface(Bridge(), "RQ")
        setContentView(web)
        web.loadUrl("file:///android_asset/index.html")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), micRequest)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.UK
            tts?.setSpeechRate(0.92f)
            tts?.setPitch(0.92f)
        }
    }

    inner class Bridge {
        @JavascriptInterface fun speak(text: String) {
            runOnUiThread { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "rq_${System.currentTimeMillis()}") }
        }
        @JavascriptInterface fun stopSpeaking() { runOnUiThread { tts?.stop() } }
        @JavascriptInterface fun listen() { runOnUiThread { startListening() } }
        @JavascriptInterface fun saveSettings(baseUrl: String, apiKey: String, model: String) {
            prefs.edit().putString("base", baseUrl.trim()).putString("key", apiKey.trim()).putString("model", model.trim()).apply()
        }
        @JavascriptInterface fun getSettings(): String {
            return JSONObject().put("base", prefs.getString("base", "https://api.openai.com/v1/chat/completions"))
                .put("model", prefs.getString("model", "gpt-4o-mini"))
                .put("hasKey", !prefs.getString("key", "").isNullOrBlank()).toString()
        }
        @JavascriptInterface fun ask(historyJson: String) {
            worker.execute {
                try {
                    val key = prefs.getString("key", "") ?: ""
                    if (key.isBlank()) {
                        replyToPage("AI connection is not configured. Open Settings and add your API endpoint, model, and API key. Your microphone and voice output already work.", true)
                        return@execute
                    }
                    val base = prefs.getString("base", "https://api.openai.com/v1/chat/completions") ?: "https://api.openai.com/v1/chat/completions"
                    val model = prefs.getString("model", "gpt-4o-mini") ?: "gpt-4o-mini"
                    val history = JSONArray(historyJson)
                    val messages = JSONArray()
                    messages.put(JSONObject().put("role", "system").put("content", "You are RED QUEEN, an original cinematic AI assistant inspired by a poised, precise sci-fi security intelligence. Speak in a calm, formal, intelligent, concise manner. You are not the actual copyrighted movie character. Address the user respectfully. Never claim to control devices or systems you cannot access."))
                    for (i in 0 until history.length()) messages.put(history.getJSONObject(i))
                    val body = JSONObject().put("model", model).put("messages", messages).put("temperature", 0.7)
                    val conn = (URL(base).openConnection() as HttpURLConnection)
                    conn.requestMethod = "POST"
                    conn.connectTimeout = 20000
                    conn.readTimeout = 45000
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.setRequestProperty("Authorization", "Bearer $key")
                    conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                    val code = conn.responseCode
                    val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                    val raw = stream.bufferedReader().use { it.readText() }
                    if (code !in 200..299) throw Exception("AI service returned HTTP $code: ${raw.take(300)}")
                    val answer = JSONObject(raw).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
                    replyToPage(answer, false)
                    conn.disconnect()
                } catch (e: Exception) {
                    replyToPage("Connection error: ${e.message ?: "Unknown error"}. Check your internet connection and API settings.", true)
                }
            }
        }
    }

    private fun replyToPage(text: String, error: Boolean) {
        runOnUiThread {
            val safe = JSONObject.quote(text)
            web.evaluateJavascript("window.onRedQueenReply($safe, $error)", null)
        }
    }

    private fun startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), micRequest)
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            replyToPage("Speech recognition is not available on this device. You can still type messages.", true)
            return
        }
        speech?.destroy()
        speech = SpeechRecognizer.createSpeechRecognizer(this)
        speech?.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { web.evaluateJavascript("window.onListening(true)", null) }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { web.evaluateJavascript("window.onListening(false)", null) }
            override fun onError(error: Int) { web.evaluateJavascript("window.onListening(false);window.onSpeechError()", null) }
            override fun onResults(results: Bundle?) {
                web.evaluateJavascript("window.onListening(false)", null)
                val words = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                web.evaluateJavascript("window.onSpeechResult(${JSONObject.quote(words)})", null)
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-GB")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Red Queen")
        }
        speech?.startListening(intent)
    }

    override fun onDestroy() {
        speech?.destroy()
        tts?.stop(); tts?.shutdown()
        worker.shutdownNow()
        web.destroy()
        super.onDestroy()
    }
}
