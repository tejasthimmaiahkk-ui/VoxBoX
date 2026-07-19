package me.thimmaiah.voxbox.speech

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

data class SpeechCapability(
    val isAvailable: Boolean,
    val usesOnDeviceRecognizer: Boolean,
)

interface SpeechRecognitionCallbacks {
    fun onReady()
    fun onBeginningOfSpeech()
    fun onPartialResult(text: String)
    fun onFinalResult(text: String)
    fun onError(message: String)
    fun onEndOfSpeech()
    fun onRecognizerModeChanged(usesOnDeviceRecognizer: Boolean, message: String)
}

class SpeechRecognitionController(
    context: Context,
    private val callbacks: SpeechRecognitionCallbacks,
) {
    private val appContext = context.applicationContext
    private var recognizer: SpeechRecognizer? = null
    private var capability = SpeechCapability(isAvailable = false, usesOnDeviceRecognizer = false)

    fun initialize(): SpeechCapability {
        destroy()
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            capability = SpeechCapability(isAvailable = false, usesOnDeviceRecognizer = false)
            return capability
        }

        val onDeviceAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(appContext)
        recognizer = if (onDeviceAvailable) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(appContext)
        } else {
            SpeechRecognizer.createSpeechRecognizer(appContext)
        }
        recognizer?.setRecognitionListener(listener)
        capability = SpeechCapability(isAvailable = true, usesOnDeviceRecognizer = onDeviceAvailable)
        return capability
    }

    fun startListening() {
        val activeRecognizer = recognizer ?: run {
            callbacks.onError("Speech recognition is not available on this device.")
            return
        }
        activeRecognizer.startListening(recognitionIntent(preferOffline = capability.usesOnDeviceRecognizer))
    }

    fun stopListening() {
        recognizer?.stopListening()
    }

    fun cancel() {
        recognizer?.cancel()
    }

    fun destroy() {
        recognizer?.destroy()
        recognizer = null
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = callbacks.onReady()
        override fun onBeginningOfSpeech() = callbacks.onBeginningOfSpeech()
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = callbacks.onEndOfSpeech()

        override fun onError(error: Int) {
            if (
                capability.usesOnDeviceRecognizer &&
                error in setOf(
                    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
                    SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
                )
            ) {
                switchToSystemRecognizer()
            } else {
                callbacks.onError(errorMessage(error))
            }
        }

        override fun onResults(results: Bundle?) {
            callbacks.onFinalResult(bestResult(results))
        }

        override fun onPartialResults(partialResults: Bundle?) {
            callbacks.onPartialResult(bestResult(partialResults))
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun bestResult(bundle: Bundle?): String = bundle
        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        ?.firstOrNull()
        .orEmpty()

    private fun recognitionIntent(preferOffline: Boolean) =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, preferOffline)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

    private fun switchToSystemRecognizer() {
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(appContext).also {
            it.setRecognitionListener(listener)
        }
        capability = SpeechCapability(isAvailable = true, usesOnDeviceRecognizer = false)
        callbacks.onRecognizerModeChanged(
            usesOnDeviceRecognizer = false,
            message = "On-device language unavailable; Android system recognizer is ready.",
        )
    }

    private fun errorMessage(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording failed."
        SpeechRecognizer.ERROR_CLIENT -> "The listening session was cancelled."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "The speech service could not reach its recognition engine."
        SpeechRecognizer.ERROR_NO_MATCH -> "No clear speech was recognized. Please try again."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "The speech recognizer is busy. Try again."
        SpeechRecognizer.ERROR_SERVER, SpeechRecognizer.ERROR_SERVER_DISCONNECTED ->
            "The speech-recognition service is unavailable."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech was detected."
        SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "Too many speech requests. Wait and retry."
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "The selected language is not supported."
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "The selected language is unavailable."
        else -> "Speech recognition failed (error $error)."
    }
}
