package com.rendy.customlockscreen.kids

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Reads the quiz question aloud so a child who cannot read yet can still use the
 * lock screen.
 *
 * Fails silently by design: if TextToSpeech is missing or fails to initialise,
 * [speak] does nothing and the lock screen behaves exactly as it did before.
 */
class QuizSpeaker(context: Context) {

    private var tts: TextToSpeech? = null
    private var ready = false
    private var languageApplied = false
    private var pending: String? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                pending?.let { speak(it) }
            } else {
                Log.w(TAG, "TextToSpeech unavailable; read-aloud disabled")
            }
            pending = null
        }
    }

    /** Speaks [text], replacing anything already queued. No-op if TTS is unavailable. */
    fun speak(text: String?) {
        if (text.isNullOrBlank()) return

        val engine = tts ?: return
        if (!ready) {
            // Still initialising; remember the last question and speak it once ready.
            pending = text
            return
        }

        if (!languageApplied) {
            // Question content is English even though the rest of the UI is Indonesian.
            val result = engine.setLanguage(Locale.ENGLISH)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                engine.setLanguage(Locale.getDefault())
            }
            languageApplied = true
        }

        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
        pending = null
    }

    private companion object {
        const val TAG = "QuizSpeaker"
        const val UTTERANCE_ID = "quiz_question"
    }
}
