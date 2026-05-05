package com.alberto.edubus.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// DEFINICIÓN DE LA CLASE
data class PalabraEducativa(
    val palabra: String,
    val pista: String
)

class MinijuegosViewModel(application: Application) : AndroidViewModel(application) {

    private val _palabraActual = MutableStateFlow(PalabraEducativa("", ""))
    val palabraActual = _palabraActual.asStateFlow()

    private val _palabraDesordenada = MutableStateFlow("")
    val palabraDesordenada = _palabraDesordenada.asStateFlow()

    private val _juegoTerminado = MutableStateFlow(false)
    val juegoTerminado = _juegoTerminado.asStateFlow()

    // --- Control de puntos ---
    private val _puntuacion = MutableStateFlow(0)
    val puntuacion = _puntuacion.asStateFlow()

    private val perfilViewModel = PerfilViewModel()

    private var listaPalabras: List<PalabraEducativa> = emptyList()
    private var indiceActual = 0

    init {
        cargarDiccionario()
    }

    private fun cargarDiccionario() {
        viewModelScope.launch {
            try {
                val jsonString = getApplication<Application>().assets
                    .open("diccionario.json")
                    .bufferedReader()
                    .use { it.readText() }

                val tipoLista = object : TypeToken<List<PalabraEducativa>>() {}.type
                listaPalabras = Gson().fromJson<List<PalabraEducativa>>(jsonString, tipoLista).shuffled()

                if (listaPalabras.isNotEmpty()) {
                    siguientePalabra()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                listaPalabras = listOf(PalabraEducativa("ERROR", "No se pudo cargar el diccionario"))
                siguientePalabra()
            }
        }
    }

    fun siguientePalabra() {
        if (indiceActual < listaPalabras.size) {
            val nuevaPalabra = listaPalabras[indiceActual]
            _palabraActual.value = nuevaPalabra

            // Lógica para desordenar las letras
            val letras = nuevaPalabra.palabra.uppercase().filter { !it.isWhitespace() }.toList().shuffled()
            _palabraDesordenada.value = letras.joinToString(" ")

            indiceActual++
        } else if (listaPalabras.isNotEmpty()) {
            _juegoTerminado.value = true
        }
    }

    fun comprobarRespuesta(respuesta: String): Boolean {
        val correcta = _palabraActual.value.palabra.trim()

        return if (respuesta.trim().equals(correcta, ignoreCase = true)) {

            // --- NUEVO SISTEMA DE PUNTUACIÓN EQUILIBRADO ---
            // 1 punto de XP por cada letra de la palabra
            val puntosGanados = correcta.length
            _puntuacion.value += puntosGanados

            siguientePalabra()
            true
        } else {
            false
        }
    }

    fun saltarPalabra() {
        // Al saltar no restamos puntos, pero pierden la oportunidad de sumarlos
        siguientePalabra()
    }

    // --- Autoguardado al cerrarse la pantalla (cuando llegas a la parada) ---
    override fun onCleared() {
        super.onCleared()
        if (_puntuacion.value > 0) {
            perfilViewModel.sumarPuntos(_puntuacion.value)
        }
    }
}