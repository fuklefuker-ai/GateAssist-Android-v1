package com.gateassist.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech
    private var ttsReady = false
    private lateinit var output: AnnouncementOutput
    private var sequence: List<SpeechItem> = emptyList()
    private var sequenceIndex = 0

    private lateinit var flightSpinner: Spinner
    private lateinit var typeSpinner: Spinner
    private lateinit var gateInput: EditText
    private lateinit var timeInput: EditText
    private lateinit var extraInput: EditText
    private lateinit var extraLabel: TextView
    private lateinit var timeLabel: TextView
    private lateinit var statusText: TextView
    private lateinit var englishText: TextView
    private lateinit var japaneseText: TextView
    private lateinit var cantoneseText: TextView
    private lateinit var confirmCheck: CheckBox
    private lateinit var enCheck: CheckBox
    private lateinit var jaCheck: CheckBox
    private lateinit var yueCheck: CheckBox
    private lateinit var playEnglish: Button
    private lateinit var playJapanese: Button
    private lateinit var playCantonese: Button
    private lateinit var playAll: Button

    private data class SpeechItem(val text: String, val locale: Locale, val id: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tts = TextToSpeech(this, this)

        flightSpinner = findViewById(R.id.flightSpinner)
        typeSpinner = findViewById(R.id.typeSpinner)
        gateInput = findViewById(R.id.gateInput)
        timeInput = findViewById(R.id.timeInput)
        extraInput = findViewById(R.id.extraInput)
        extraLabel = findViewById(R.id.extraLabel)
        timeLabel = findViewById(R.id.timeLabel)
        statusText = findViewById(R.id.statusText)
        englishText = findViewById(R.id.englishText)
        japaneseText = findViewById(R.id.japaneseText)
        cantoneseText = findViewById(R.id.cantoneseText)
        confirmCheck = findViewById(R.id.confirmCheck)
        enCheck = findViewById(R.id.enCheck)
        jaCheck = findViewById(R.id.jaCheck)
        yueCheck = findViewById(R.id.yueCheck)
        playEnglish = findViewById(R.id.playEnglish)
        playJapanese = findViewById(R.id.playJapanese)
        playCantonese = findViewById(R.id.playCantonese)
        playAll = findViewById(R.id.playAll)

        val flightLabels = AnnouncementEngine.flights.map { "${it.number} · ${it.destination} · Gate ${it.gate}" }
        flightSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, flightLabels)
        typeSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, AnnouncementType.entries.map { it.label })

        flightSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val f = AnnouncementEngine.flights[position]
                gateInput.setText(f.gate)
                val type = AnnouncementType.entries.getOrElse(typeSpinner.selectedItemPosition) { AnnouncementType.BOARDING }
                timeInput.setText(if (type == AnnouncementType.DELAY) f.departure else f.close)
                resetConfirmation()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }

        typeSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val type = AnnouncementType.entries[position]
                updateDynamicField(type)
                val f = AnnouncementEngine.flights[flightSpinner.selectedItemPosition]
                timeInput.setText(if (type == AnnouncementType.DELAY) f.departure else f.close)
                resetConfirmation()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }

        findViewById<Button>(R.id.generateButton).setOnClickListener { generate() }
        findViewById<Button>(R.id.ttsSettingsButton).setOnClickListener {
            startActivity(Intent(Settings.ACTION_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
        }
        confirmCheck.setOnCheckedChangeListener { _, checked -> setPlaybackEnabled(checked && ::output.isInitialized && output.warnings.isEmpty()) }
        enCheck.setOnCheckedChangeListener { _, _ -> setPlaybackEnabled(confirmCheck.isChecked && ::output.isInitialized && output.warnings.isEmpty()) }
        jaCheck.setOnCheckedChangeListener { _, _ -> setPlaybackEnabled(confirmCheck.isChecked && ::output.isInitialized && output.warnings.isEmpty()) }
        yueCheck.setOnCheckedChangeListener { _, _ -> setPlaybackEnabled(confirmCheck.isChecked && ::output.isInitialized && output.warnings.isEmpty()) }

        playEnglish.setOnClickListener { speakSingle(output.english, Locale.US, "en") }
        playJapanese.setOnClickListener { speakSingle(output.japanese, Locale.JAPAN, "ja") }
        playCantonese.setOnClickListener { speakSingle(output.cantonese, Locale.forLanguageTag("yue-HK"), "yue") }
        playAll.setOnClickListener { playSequence() }
        findViewById<Button>(R.id.stopButton).setOnClickListener { stopSpeech() }
    }

    private fun updateDynamicField(type: AnnouncementType) {
        timeLabel.text = if (type == AnnouncementType.DELAY) "Updated departure time" else "Boarding close time"
        timeInput.hint = if (type == AnnouncementType.DELAY) "e.g. 18:15" else "e.g. 17:10"
        extraLabel.text = when (type) {
            AnnouncementType.PAGING -> "Passenger name"
            AnnouncementType.GATE_CHANGE -> "New gate"
            AnnouncementType.VOLUNTEER -> "Alternative flight / details"
            else -> "Extra details (optional)"
        }
        extraInput.hint = when (type) {
            AnnouncementType.PAGING -> "Passenger name"
            AnnouncementType.GATE_CHANGE -> "New gate number"
            AnnouncementType.VOLUNTEER -> "Alternative flight/details"
            else -> "Not required for this announcement"
        }
        if (type !in listOf(AnnouncementType.PAGING, AnnouncementType.GATE_CHANGE, AnnouncementType.VOLUNTEER)) extraInput.setText("")
    }

    private fun resetConfirmation() {
        confirmCheck.isChecked = false
        confirmCheck.isEnabled = false
        setPlaybackEnabled(false)
    }

    private fun generate() {
        stopSpeech()
        val f = AnnouncementEngine.flights[flightSpinner.selectedItemPosition]
        val type = AnnouncementType.entries[typeSpinner.selectedItemPosition]
        output = AnnouncementEngine.generate(type, f, gateInput.text.toString().trim(), timeInput.text.toString().trim(), extraInput.text.toString().trim())
        englishText.text = output.english
        japaneseText.text = output.japanese
        cantoneseText.text = output.cantonese

        if (output.warnings.isEmpty()) {
            statusText.text = "✓ Basic validation passed. Confirm the operational details before playback."
            confirmCheck.isEnabled = true
        } else {
            statusText.text = output.warnings.joinToString("\n") { "⚠ $it" } + "\nCorrect the details and generate again."
            confirmCheck.isEnabled = false
        }
        confirmCheck.isChecked = false
        setPlaybackEnabled(false)
    }

    private fun setPlaybackEnabled(enabled: Boolean) {
        playEnglish.isEnabled = enabled && enCheck.isChecked
        playJapanese.isEnabled = enabled && jaCheck.isChecked
        playCantonese.isEnabled = enabled && yueCheck.isChecked
        playAll.isEnabled = enabled && (enCheck.isChecked || jaCheck.isChecked || yueCheck.isChecked)
    }

    private fun speakSingle(text: String, locale: Locale, id: String) {
        stopSpeech()
        if (!ttsReady) {
            Toast.makeText(this, "Text-to-speech is not ready on this device.", Toast.LENGTH_SHORT).show()
            return
        }
        if (!prepareLanguage(locale)) return
        val actual = if (locale.language == "en") AnnouncementEngine.speechSafeEnglish(text) else text
        tts.speak(actual, TextToSpeech.QUEUE_FLUSH, null, id)
    }

    private fun prepareLanguage(locale: Locale): Boolean {
        val result = tts.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Toast.makeText(this, "${locale.displayLanguage} voice is not installed. Add the language in your Android text-to-speech settings.", Toast.LENGTH_LONG).show()
            return false
        }
        tts.setSpeechRate(0.90f)
        return true
    }

    private fun playSequence() {
        if (!ttsReady || !::output.isInitialized) return
        stopSpeech()
        sequence = buildList {
            if (enCheck.isChecked) add(SpeechItem(AnnouncementEngine.speechSafeEnglish(output.english), Locale.US, "seq-en"))
            if (jaCheck.isChecked) add(SpeechItem(output.japanese, Locale.JAPAN, "seq-ja"))
            if (yueCheck.isChecked) add(SpeechItem(output.cantonese, Locale.forLanguageTag("yue-HK"), "seq-yue"))
        }
        sequenceIndex = 0
        speakNextInSequence()
    }

    private fun speakNextInSequence() {
        if (sequenceIndex >= sequence.size) return
        val item = sequence[sequenceIndex]
        if (!prepareLanguage(item.locale)) {
            sequenceIndex++
            speakNextInSequence()
            return
        }
        tts.speak(item.text, TextToSpeech.QUEUE_FLUSH, null, item.id)
    }

    private fun stopSpeech() {
        sequence = emptyList()
        sequenceIndex = 0
        if (::tts.isInitialized) tts.stop()
    }

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        if (!ttsReady) {
            Toast.makeText(this, "Android text-to-speech could not initialize.", Toast.LENGTH_LONG).show()
            return
        }
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onError(utteranceId: String?) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Audio playback failed.", Toast.LENGTH_SHORT).show() }
            }
            override fun onDone(utteranceId: String?) {
                if (utteranceId?.startsWith("seq-") == true && sequence.isNotEmpty()) {
                    sequenceIndex++
                    runOnUiThread { speakNextInSequence() }
                }
            }
        })
    }

    override fun onDestroy() {
        stopSpeech()
        tts.shutdown()
        super.onDestroy()
    }
}
