package com.hossein.jarvisfa

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color
import android.view.Gravity
import android.net.Uri
import android.provider.ContactsContract
import android.content.pm.PackageManager
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech
    private lateinit var status: TextView
    private val requestCodeSpeech = 101
    private val requestContacts = 102
    private var pendingAction = ""
    private var pendingNumber = ""
    private var pendingName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 40, 28, 28)
            setBackgroundColor(Color.rgb(13, 20, 35))
        }
        val title = TextView(this).apply { text = "JARVIS"; textSize = 34f; setTextColor(Color.WHITE); gravity = Gravity.CENTER }
        status = TextView(this).apply {
            text = "سلام! من جارویس هستم. برای صحبت دکمه را بزن."
            textSize = 18f; setTextColor(Color.LTGRAY); gravity = Gravity.CENTER; setPadding(0, 32, 0, 32)
        }
        val listen = Button(this).apply { text = "🎙 صحبت با جارویس"; textSize = 18f; setOnClickListener { startListening() } }
        layout.addView(title); layout.addView(status); layout.addView(listen)
        setContentView(layout)
    }

    private fun startListening(prompt: String = "با جارویس صحبت کن") {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
            putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
        }
        try { startActivityForResult(intent, requestCodeSpeech) }
        catch (_: Exception) { reply("تشخیص گفتار روی این گوشی در دسترس نیست.") }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestCodeSpeech && resultCode == RESULT_OK) {
            val words = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            status.text = "گفتی: " + words
            if (pendingAction == "sms_body") { pendingAction = ""; confirmSms(words.trim()) } else handleCommand(words)
        }
    }

    private fun handleCommand(raw: String) {
        val q = raw.lowercase(Locale("fa", "IR")).replace("‌", " ")
        when {
            q.contains("پیام بده") || q.contains("پیامک بده") || q.contains("اس ام اس") -> {
                val name = q.substringAfter("به ", "").substringBefore(" پیام").trim()
                if (name.isBlank()) reply("نام مخاطب را هم بگو؛ مثلاً به زهرا پیام بده") else findContact(name, "sms")
            }
            q.contains("زنگ بزن") || q.contains("تماس بگیر") -> {
                val name = q.substringAfter("به ", "").trim()
                if (name.isBlank()) reply("به چه کسی زنگ بزنم؟") else findContact(name, "call")
            }
            q.contains("کروم") || q.contains("گوگل") || q.contains("مرورگر") -> openApp("com.android.chrome", "مرورگر")
            q.contains("تنظیمات") -> openApp("com.android.settings", "تنظیمات")
            q.contains("یوتیوب") -> openApp("com.google.android.youtube", "یوتیوب")
            q.contains("ساعت") || q.contains("آلارم") || q.contains("زنگ") -> openApp("com.google.android.deskclock", "ساعت")
            q.contains("باتری") -> {
                val battery = registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val level = battery?.getIntExtra("level", -1) ?: -1
                reply(if (level >= 0) "شارژ باتری " + level + " درصد است" else "درصد باتری مشخص نشد")
            }
            q.contains("سلام") -> reply("سلام! چه کاری برات انجام بدم؟")
            q.contains("خداحافظ") -> reply("خدانگهدار!")
            else -> reply("این فرمان را هنوز بلد نیستم.")
        }
    }

    private fun findContact(spokenName: String, action: String) {
        if (checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            pendingAction = action; pendingName = spokenName
            requestPermissions(arrayOf(android.Manifest.permission.READ_CONTACTS), requestContacts)
            return
        }
        val found = lookupContact(spokenName)
        if (found == null) { reply("مخاطبی با نام " + spokenName + " پیدا نکردم."); return }
        pendingName = found.first; pendingNumber = found.second; pendingAction = action
        if (action == "call") {
            AlertDialog.Builder(this).setTitle("تأیید تماس")
                .setMessage("با " + found.first + " تماس گرفته شود؟")
                .setNegativeButton("لغو") { _, _ -> pendingAction = ""; reply("تماس لغو شد") }
                .setPositiveButton("ادامه") { _, _ ->
                    pendingAction = ""
                    startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + found.second)))
                }.show()
        } else {
            pendingAction = "sms_body"
            reply("متن پیامت را بگو.")
            startListening("متن پیام به " + found.first + " را بگو")
        }
    }

    private fun lookupContact(query: String): Pair<String, String>? {
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER)
        contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, null, null, null)?.use { c ->
            val ni = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val pi = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (c.moveToNext()) {
                val name = c.getString(ni) ?: continue
                val number = c.getString(pi) ?: continue
                if (name.contains(query, true) || query.contains(name, true)) return Pair(name, number)
            }
        }
        return null
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestContacts) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val action = pendingAction; val name = pendingName; pendingAction = ""
                findContact(name, action)
            } else { pendingAction = ""; reply("برای پیدا کردن مخاطب، اجازه دسترسی به مخاطبین لازم است.") }
        }
    }

    private fun confirmSms(body: String) {
        if (body.isBlank()) { reply("متن پیام خالی بود."); return }
        AlertDialog.Builder(this).setTitle("تأیید پیام")
            .setMessage("پیام برای " + pendingName + " آماده شود؟\n\n" + body)
            .setNegativeButton("لغو") { _, _ -> reply("پیام لغو شد") }
            .setPositiveButton("باز کردن پیامک‌ها") { _, _ ->
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + pendingNumber)).apply { putExtra("sms_body", body) }
                startActivity(intent)
            }.show()
    }

    private fun openApp(packageName: String, label: String) {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        if (launch != null) { startActivity(launch); reply(label + " را باز می‌کنم") } else reply(label + " روی گوشی پیدا نشد")
    }
    private fun reply(message: String) { status.text = message; speak(message) }
    private fun speak(message: String) { if (::tts.isInitialized) tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "jarvis_reply") }
    override fun onInit(statusCode: Int) { if (statusCode == TextToSpeech.SUCCESS) tts.language = Locale("fa", "IR") }
    override fun onDestroy() { if (::tts.isInitialized) { tts.stop(); tts.shutdown() }; super.onDestroy() }
}
