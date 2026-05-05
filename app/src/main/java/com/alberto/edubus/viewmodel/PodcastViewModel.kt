package com.alberto.edubus.viewmodel

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit // <-- IMPORTANTE: Añadida esta línea
import kotlin.collections.get

class PodcastViewModel : ViewModel() {
    private var mediaPlayer: MediaPlayer? = null

    // Conexión a Firebase
    private val db = FirebaseFirestore.getInstance()

    private val _estaReproduciendo = MutableStateFlow(false)
    val estaReproduciendo = _estaReproduciendo.asStateFlow()

    private val _progreso = MutableStateFlow(0f)
    val progreso: StateFlow<Float> = _progreso.asStateFlow()

    private val _temaActual = MutableStateFlow("Cargando podcast...")
    val temaActual = _temaActual.asStateFlow()

    fun inicializarPodcast(temaId: String) {
        _temaActual.value = "Conectando con la IA..."

        // Buscamos en Firestore la URL del MP3 generado hoy
        db.collection("podcasts").document(temaId)
            .get()
            .addOnSuccessListener { documento ->
                if (documento != null && documento.exists()) {
                    val urlReal = documento.getString("audioUrl")
                    val titulo = documento.getString("tituloHoy") ?: "Podcast de $temaId"

                    if (urlReal != null) {
                        _temaActual.value = titulo
                        prepararMediaPlayer(urlReal)
                    } else {
                        _temaActual.value = "Audio no disponible hoy"
                    }
                } else {
                    _temaActual.value = "Podcast no encontrado"
                }
            }
            .addOnFailureListener { e ->
                _temaActual.value = "Error de conexión"
                Log.e("EduBus", "Error al conectar con Firestore", e)
            }
    }

    private fun prepararMediaPlayer(url: String) {
        mediaPlayer?.release()

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            try {
                setDataSource(url)
                prepareAsync() // Descarga el audio en segundo plano

                setOnPreparedListener {
                    Log.d("EduBus", "Audio cargado y listo para sonar")
                }

                // Cuando termina el podcast, resetea el botón a "Play"
                setOnCompletionListener {
                    _estaReproduciendo.value = false
                    _progreso.value = 0f
                }
            } catch (e: Exception) {
                Log.e("EduBus", "Error al cargar el audio", e)
                _temaActual.value = "Error en el reproductor"
            }
        }
    }

    fun toggleReproduccion(context: Context) {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                _estaReproduciendo.value = false
            } else {
                player.start()
                _estaReproduciendo.value = true
                actualizarProgreso()
            }
        }
    }

    private fun actualizarProgreso() {
        viewModelScope.launch {
            while (_estaReproduciendo.value && mediaPlayer != null) {
                val actual = mediaPlayer!!.currentPosition.toFloat()
                val total = mediaPlayer!!.duration.toFloat()
                if (total > 0) _progreso.value = actual / total
                delay(1000)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    fun crearPodcastAMedida(temaId: String, segundosParaLlegar: Int) {
        _temaActual.value = "La IA está escribiendo tu guion..."

        val datos = hashMapOf(
            "temaId" to temaId,
            "segundosRestantes" to segundosParaLlegar
        )

        FirebaseFunctions.getInstance()
            .getHttpsCallable("generarPodcastInstantaneo")
            .withTimeout(120, TimeUnit.SECONDS) // <-- LA CLAVE: Le damos 120 segundos de margen
            .call(datos)
            .addOnSuccessListener { result ->
                val data = result.data as Map<*, *>
                val url = data["audioUrl"] as String
                val nuevoTitulo = data["titulo"] as String

                _temaActual.value = nuevoTitulo
                prepararMediaPlayer(url)
            }
            .addOnFailureListener { e ->
                _temaActual.value = "Error al crear podcast"
                // <-- Añadimos este log para ver en el Logcat qué falla exactamente si vuelve a pasar
                Log.e("EduBus", "Error conectando con Cloud Functions", e)
            }
    }
}