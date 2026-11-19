package ir.jarvis.fa

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var status: TextView
    private lateinit var clock: TextView
    private lateinit var battery: TextView
    private var tts: TextToSpeech? = null
    private val handler = Handler(Looper.getMainLooper())
    private val requestCode = 71
    private val navy = Color.rgb(8, 15, 30)
    private val cyan = Color.rgb(63, 220, 235)
    private val white = Color.rgb(235, 246, 255)

    private val ticker = object : Runnable {
        override fun run() {
            clock.text = SimpleDateFormat("HH:mm", Locale("fa", "IR")).format(Date())
            val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
            battery.text = "باتری " + bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) + "٪"
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = navy
        window.navigationBarColor = navy
        tts = TextToSpeech(this, this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 28, 24, 24)
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(5, 12, 27), Color.rgb(12, 34, 55), Color.rgb(5, 12, 27)))
        }
        val brand = TextView(this).apply {
            text = "J.A.R.V.I.S"
            textSize = 13f
            letterSpacing = 0.32f
            setTextColor(cyan)
            gravity = Gravity.CENTER
        }
        val title = TextView(this).apply {
            text = "جارویس"
            textSize = 34f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setTextColor(white)
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 2)
        }
        val subtitle = TextView(this).apply {
            text = "دستیار هوشمند فارسی"
            textSize = 14f
            setTextColor(Color.rgb(150, 180, 198))
            gravity = Gravity.CENTER
        }
        val orb = TextView(this).apply {
            text = "◉"
            textSize = 66f
            setTextColor(cyan)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(15, 49, 68))
                setStroke(3, cyan)
            }
            elevation = 14f
            setOnClickListener { listen() }
        }
        val orbParams = LinearLayout.LayoutParams(190, 190).apply {
            topMargin = 34
            bottomMargin = 16
            gravity = Gravity.CENTER
        }
        status = TextView(this).apply {
            text = "برای صحبت، دایره‌ی وسط را لمس کن"
            textSize = 16f
            setTextColor(white)
            gravity = Gravity.CENTER
            setPadding(12, 18, 12, 18)
            background = panel()
        }
        val infoRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        clock = infoText("۰۰:۰۰")
        battery = infoText("باتری --٪")
        infoRow.addView(infoCard(clock), LinearLayout.LayoutParams(0, 68, 1f).apply { rightMargin = 6 })
        infoRow.addView(infoCard(battery), LinearLayout.LayoutParams(0, 68, 1f).apply { leftMargin = 6 })
        val quickTitle = TextView(this).apply {
            text = "دسترسی سریع"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(white)
            gravity = Gravity.RIGHT
            setPadding(0, 24, 0, 10)
        }
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        actions.addView(actionButton("☎ تماس") { openDialer() }, LinearLayout.LayoutParams(0, 54, 1f).apply { rightMargin = 5 })
        actions.addView(actionButton("✉ پیامک") { openSms() }, LinearLayout.LayoutParams(0, 54, 1f).apply { leftMargin = 5; rightMargin = 5 })
        actions.addView(actionButton("♫ موسیقی") { openMusic() }, LinearLayout.LayoutParams(0, 54, 1f).apply { leftMargin = 5 })
        val hint = TextView(this).apply {
            text = "نمونه فرمان: «سلام جارویس» • «موسیقی را باز کن»"
            textSize = 12f
            setTextColor(Color.rgb(130, 158, 178))
            gravity = Gravity.CENTER
            setPadding(4, 22, 4, 0)
        }
        root.addView(brand)
        root.addView(title)
        root.addView(subtitle)
        root.addView(orb, orbParams)
        root.addView(status, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 4 })
        root.addView(infoRow, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 18 })
        root.addView(quickTitle)
        root.addView(actions)
        root.addView(hint)
        setContentView(root)
        handler.post(ticker)
    }

    private fun panel() = GradientDrawable().apply {
        cornerRadius = 24f
        setColor(Color.rgb(17, 37, 55))
        setStroke(1, Color.rgb(37, 91, 112))
    }
    private fun infoText(value: String) = TextView(this).apply {
        text = value
        textSize = 18f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(cyan)
        gravity = Gravity.CENTER
    }
    private fun infoCard(child: View) = LinearLayout(this).apply {
        gravity = Gravity.CENTER
        background = panel()
        setPadding(8, 4, 8, 4)
        addView(child)
    }
    private fun actionButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        textSize = 12f
        isAllCaps = false
        setTextColor(white)
        background = panel()
        setOnClickListener { action() }
    }

    override fun onInit(code: Int) {
        if (code == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("fa", "IR"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                status.text = "صدای فارسی روی گوشی نصب نیست؛ متن همچنان نمایش داده می‌شود."
            }
        }
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
            val command = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            handle(command)
        }
    }
    private fun handle(raw: String) {
        val command = raw.replace("‌", " ").trim()
        when {
            command.contains("زنگ بزن") || command.contains("تماس بگیر") -> {
                say("شماره‌گیر باز می‌شود؛ مخاطب را انتخاب کن و تماس را خودت تأیید کن.")
                openDialer()
            }
            command.contains("پیامک") || command.contains("پیام بده") -> {
                say("برنامه پیامک باز می‌شود؛ گیرنده و متن را بررسی کن و خودت ارسال را بزن.")
                openSms()
            }
            command.contains("آهنگ") || command.contains("موسیقی") -> {
                say("پخش‌کننده موسیقی را باز می‌کنم.")
                openMusic()
            }
            command.contains("ساعت") || command.contains("زمان") -> say("ساعت " + clock.text)
            command.contains("باتری") -> say(battery.text)
            command.contains("سلام") -> say("سلام! جارویس آماده‌ی خدمته.")
            else -> say("این فرمان را هنوز یاد نگرفته‌ام.")
        }
    }
    private fun openDialer() {
        try { startActivity(Intent(Intent.ACTION_DIAL)) } catch (_: Exception) { say("شماره‌گیر پیدا نشد.") }
    }
    private fun openSms() {
        try { startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))) }
        catch (_: Exception) { say("برنامه پیامک پیدا نشد.") }
    }
    private fun openMusic() {
        val music = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC)
        try { startActivity(music) } catch (_: Exception) { say("پخش‌کننده‌ای پیدا نشد.") }
    }
    private fun say(message: String) {
        status.text = message
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "jarvis")
    }
    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
