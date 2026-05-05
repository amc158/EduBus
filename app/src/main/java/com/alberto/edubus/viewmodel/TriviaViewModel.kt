package com.alberto.edubus.viewmodel

import android.text.Html
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alberto.edubus.model.TriviaQuestion
import com.alberto.edubus.network.RetrofitClient
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale

class TriviaViewModel : ViewModel() {

    private val _preguntas = MutableStateFlow<List<TriviaQuestion>>(emptyList())
    val preguntas: StateFlow<List<TriviaQuestion>> = _preguntas.asStateFlow()

    private val _cargando = MutableStateFlow(true)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _mensajeCarga = MutableStateFlow("Preparando juego...")
    val mensajeCarga: StateFlow<String> = _mensajeCarga.asStateFlow()

    // --- NUEVO: Puntuación controlada ---
    private val _puntuacion = MutableStateFlow(0)
    val puntuacion = _puntuacion.asStateFlow()
    private val perfilViewModel = PerfilViewModel()

    private var translator: Translator? = null

    init { configurarTraductor() }

    private fun configurarTraductor() {
        val idiomaDispositivo = Locale.getDefault().language
        val targetLang = TranslateLanguage.fromLanguageTag(idiomaDispositivo) ?: TranslateLanguage.SPANISH
        if (targetLang == TranslateLanguage.ENGLISH) return
        val options = TranslatorOptions.Builder().setSourceLanguage(TranslateLanguage.ENGLISH).setTargetLanguage(targetLang).build()
        translator = Translation.getClient(options)
    }

    fun cargarPreguntas(cantidad: Int) {
        viewModelScope.launch {
            _cargando.value = true
            try {
                _mensajeCarga.value = "Descargando preguntas..."
                val response = RetrofitClient.triviaApi.getPreguntas(cantidad)
                val listaOriginal = response.results

                if (translator != null) {
                    _mensajeCarga.value = "Traduciendo con Inteligencia Artificial..."
                    val conditions = DownloadConditions.Builder().build()
                    translator?.downloadModelIfNeeded(conditions)?.await()

                    val listaTraducida = mutableListOf<TriviaQuestion>()
                    for (pregunta in listaOriginal) {
                        val pLimpia = decodificarTexto(pregunta.question)
                        val pTraducida = translator?.translate(pLimpia)?.await() ?: pLimpia
                        val rCorrectaLimpia = decodificarTexto(pregunta.correctAnswer)
                        val rCorrectaTraducida = translator?.translate(rCorrectaLimpia)?.await() ?: rCorrectaLimpia
                        val incorrectasTraducidas = mutableListOf<String>()
                        for (incorrecta in pregunta.incorrectAnswers) {
                            val incLimpia = decodificarTexto(incorrecta)
                            incorrectasTraducidas.add(translator?.translate(incLimpia)?.await() ?: incLimpia)
                        }
                        listaTraducida.add(pregunta.copy(question = pTraducida, correctAnswer = rCorrectaTraducida, incorrectAnswers = incorrectasTraducidas))
                    }
                    _preguntas.value = listaTraducida
                } else {
                    _preguntas.value = listaOriginal.map { p ->
                        p.copy(question = decodificarTexto(p.question), correctAnswer = decodificarTexto(p.correctAnswer), incorrectAnswers = p.incorrectAnswers.map { decodificarTexto(it) })
                    }
                }
            } catch (e: Exception) {
                _preguntas.value = emptyList()
            } finally { _cargando.value = false }
        }
    }

    fun sumarPuntoCorrecto() { _puntuacion.value += 10 }

    private fun decodificarTexto(texto: String): String = Html.fromHtml(texto, Html.FROM_HTML_MODE_LEGACY).toString()

    // --- NUEVO: Autoguardado ---
    override fun onCleared() {
        super.onCleared()
        translator?.close()
        if (_puntuacion.value > 0) {
            perfilViewModel.sumarPuntos(_puntuacion.value)
        }
    }
}