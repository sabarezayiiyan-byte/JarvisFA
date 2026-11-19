package com.hossein.jarvisfa

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.graphics.Color
import android.view.Gravity
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech
    private lateinit var status: TextView
    private val requestCodeSpeech = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 40, 28, 28)
            setBackgroundColor(Color.rgb(13, 20, 35))
        }
        val title = TextView(this).apply {
            text = "JARVIS"
            textSize = 34f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        status = TextView(this).apply {
            text = "سلام! من جارویس هستم. برای صحبت دکمه را بزن."
            textSize = 18f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 32)
        }
        val listen = Button(this).apply {
            text = "🎙 صحبت با جارویس"
            textSize = 18f
            setOnClickListener { startListening() }
        }
        layout.addView(title)
        layout.addView(status)
        layout.addView(listen)
        setContentView(layout)
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "با جارویس صحبت کن")
        }
        try { startActivityForResult(intent, requestCodeSpeech) }
        catch (_: Exception) {
            status.text = "تشخیص گفتار روی این گوشی در دسترس نیست."
            speak("تشخیص گفتار در دسترس نیست")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestCodeSpeech && resultCode == RESULT_OK) {
            val words = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            status.text = "گفتی: $words"
            handleCommand(words)
        }
    }

    private fun handleCommand(raw: String) {
        val q = raw.lowercase(Locale("fa", "IR"))
        when {
            q.contains("کروم") || q.contains("گوگل") || q.contains("مرورگر") -> openApp("com.android.chrome", "مرورگر")
            q.contains("تنظیمات") -> openApp("com.android.settings", "تنظیمات")
            q.contains("یوتیوب") -> openApp("com.google.android.youtube", "یوتیوب")
            q.contains("ساعت") || q.contains("آلارم") || q.contains("زنگ") -> openApp("com.google.android.deskclock", "ساعت")
            q.contains("باتری") -> {
                val battery = registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val level = battery?.getIntExtra("level", -1) ?: -1
                val answer = if (level >= 0) "شارژ باتری $level درصد است" else "درصد باتری مشخص نشد"
                status.text = answer
                speak(answer)
            }
            q.contains("سلام") -> reply("سلام! چه کاری برات انجام بدم؟")
            q.contains("خداحافظ") -> reply("خدانگهدار!")
            else -> reply("فعلاً این فرمان را بلد نیستم. می‌توانی بگویی کروم را باز کن، تنظیمات را باز کن، یا درصد باتری را بگو.")
        }
    }

    private fun openApp(packageName: String, label: String) {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        if (launch != null) {
            startActivity(launch)
            reply("$label را باز می‌کنم")
        } else {
            reply("$label روی گوشی پیدا نشد")
        }
    }

    private fun reply(message: String) {
        status.text = message
        speak(message)
    }

    private fun speak(message: String) {
        if (::tts.isInitialized) tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "jarvis_reply")
    }

    override fun onInit(statusCode: Int) {
        if (statusCode == TextToSpeech.SUCCESS) tts.language = Locale("fa", "IR")
    }

    override fun onDestroy() {
        if (::tts.isInitialized) { tts.stop(); tts.shutdown() }
        super.onDestroy()
    }
}
