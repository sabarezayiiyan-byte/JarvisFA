package ir.jarvis.fa

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var status: TextView
    private var tts: TextToSpeech? = null
    private val requestCode = 71

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 32, 28, 32)
        }
        val title = TextView(this).apply {
            text = "جارویس"
            textSize = 32f
            gravity = Gravity.CENTER
        }
        status = TextView(this).apply {
            text = "سلام! برای فرمان صوتی دکمه را بزن."
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 24)
        }
        val listen = Button(this).apply {
            text = "🎙 صحبت با جارویس"
            textSize = 18f
            setOnClickListener { listen() }
        }
        layout.addView(title)
        layout.addView(status)
        layout.addView(listen)
        setContentView(layout)
    }

    override fun onInit(code: Int) {
        if (code == TextToSpeech.SUCCESS) tts?.language = Locale("fa", "IR")
    }

    private fun listen() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), requestCode)
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "فرمانت را بگو")
        }
        try { startActivityForResult(intent, requestCode) }
        catch (_: Exception) { say("تشخیص گفتار روی این گوشی در دسترس نیست.") }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode == this.requestCode && results.isNotEmpty() && results[0] == PackageManager.PERMISSION_GRANTED) listen()
    }

    @Deprecated("Deprecated by Android")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == this.requestCode && resultCode == RESULT_OK) {
            val words = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val command = words?.firstOrNull().orEmpty()
            status.text = "شنیدم: $command"
            handle(command)
        }
    }

    private fun handle(raw: String) {
        val command = raw.replace("‌", " ").trim()
        when {
            command.contains("زنگ بزن") || command.contains("تماس بگیر") -> {
                val name = command.substringBefore("زنگ بزن").substringBefore("تماس بگیر").trim()
                say(if (name.isBlank()) "نام مخاطب را بگو." else "برای تماس با $name، مخاطب را از دفترچه تلفن انتخاب کن.")
                val dial = Intent(Intent.ACTION_DIAL)
                try { startActivity(dial) } catch (_: Exception) { say("شماره‌گیر پیدا نشد.") }
            }
            command.contains("پیامک") || command.contains("پیام بده") -> {
                say("برنامه پیامک باز می‌شود. گیرنده و متن را بررسی کن و خودت ارسال را بزن.")
                try { startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))) }
                catch (_: Exception) { say("برنامه پیامک پیدا نشد.") }
            }
            command.contains("آهنگ") || command.contains("موسیقی") -> {
                say("پخش‌کننده موسیقی را باز می‌کنم.")
                val music = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC)
                try { startActivity(music) } catch (_: Exception) { say("پخش‌کننده‌ای پیدا نشد.") }
            }
            command.contains("سلام") -> say("سلام! در خدمتم.")
            else -> say("این فرمان را هنوز یاد نگرفته‌ام.")
        }
    }

    private fun say(text: String) {
        status.text = text
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis")
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
