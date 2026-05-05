package com.alberto.edubus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alberto.edubus.model.Parada
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BusViewModel : ViewModel() {

    // MEMORIA DE RUTA: Guardamos las paradas aquí para que no se borren al volver atrás
    private val _origenSeleccionado = MutableStateFlow<Parada?>(null)
    val origenSeleccionado: StateFlow<Parada?> = _origenSeleccionado.asStateFlow()

    private val _destinoSeleccionado = MutableStateFlow<Parada?>(null)
    val destinoSeleccionado: StateFlow<Parada?> = _destinoSeleccionado.asStateFlow()

    // TIEMPO: Usamos segundos para máxima precisión
    private val _segundosRestantes = MutableStateFlow(0)
    val segundosRestantes: StateFlow<Int> = _segundosRestantes.asStateFlow()

    // Variable para controlar el hilo del cronómetro y evitar que se duplique
    private var timerJob: Job? = null

    fun establecerParadas(origen: Parada?, destino: Parada?) {
        _origenSeleccionado.value = origen
        _destinoSeleccionado.value = destino
    }

    fun calcularTiempoTotalParaActividades(esperaBus: Int, tiempoTrayecto: Int) {
        val tiempoTotalMinutos = esperaBus + tiempoTrayecto
        _segundosRestantes.value = tiempoTotalMinutos * 60

        // Iniciamos el cronómetro global que no se detiene al cambiar de pantalla
        iniciarCronometro()
    }

    private fun iniciarCronometro() {
        // Si ya hay un cronómetro funcionando, lo cancelamos antes de empezar uno nuevo
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_segundosRestantes.value > 0) {
                delay(1000L)
                _segundosRestantes.value -= 1
            }
        }
    }

    fun resetearViaje() {
        timerJob?.cancel()
        _origenSeleccionado.value = null
        _destinoSeleccionado.value = null
        _segundosRestantes.value = 0
    }
}