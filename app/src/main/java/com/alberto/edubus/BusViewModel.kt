package com.alberto.edubus

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BusViewModel : ViewModel() {

    // Estado que guarda el tiempo total calculado para que la interfaz se actualice
    private val _tiempoDisponible = MutableStateFlow(0)
    val tiempoDisponible: StateFlow<Int> = _tiempoDisponible.asStateFlow()

    /**
     * ALGORITMO DE CÁLCULO DE TIEMPO DE EDUBUS
     * * @param esperaBus: El tiempo que le queda al bus para llegar a la parada de ORIGEN.
     * @param tiempoTrayecto: El tiempo que tarda el bus en ir del origen al destino.
     */
    fun calcularTiempoTotalParaActividades(esperaBus: Int, tiempoTrayecto: Int) {

        // Sumamos lo que esperas en la parada + lo que dura tu viaje
        val tiempoTotal = esperaBus + tiempoTrayecto

        // Actualizamos el estado. Al cambiar este valor, la pantalla mostrará los minutos correctos.
        _tiempoDisponible.value = tiempoTotal
    }
}