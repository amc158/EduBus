package com.alberto.edubus.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class NivelEduBus(
    val numero: Int,
    val nombre: String,
    val puntosNecesarios: Int, // Puntos para COMPLETAR este nivel
    val puntosMinimos: Int     // Puntos para DESBLOQUEAR este nivel
)

class PerfilViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _puntosTotales = MutableStateFlow(0)
    val puntosTotales: StateFlow<Int> = _puntosTotales.asStateFlow()

    private val _nivelActual = MutableStateFlow("Calculando...")
    val nivelActual: StateFlow<String> = _nivelActual.asStateFlow()

    // --- NUEVA ESCALA DE 10 NIVELES EQUILIBRADA ---
    val listaNiveles = listOf(
        NivelEduBus(1, "Novato del Bus 🚶", 100, 0),
        NivelEduBus(2, "Lector de Paradas 🚌", 250, 100),
        NivelEduBus(3, "Viajero Curioso 🧐", 450, 250),
        NivelEduBus(4, "Copiloto Honorífico 🗺️", 700, 450),
        NivelEduBus(5, "Explorador Urbano 🌆", 1000, 700),
        NivelEduBus(6, "Maestro de Líneas 📍", 1400, 1000),
        NivelEduBus(7, "Guía de la Ciudad 🏛️", 1900, 1400),
        NivelEduBus(8, "Experto en Trayectos ⚡", 2500, 1900),
        // A partir de aquí los niveles son "Élite"
        NivelEduBus(9, "Veterano de Surbus 🎖️", 3200, 2500),
        NivelEduBus(10, "Leyenda del Transporte 👑", 5000, 3200)
    )

    init {
        cargarPerfil()
    }

    // --- FUNCIÓN PARA RESETEAR PUNTOS A CERO ---
    fun resetearPuntos() {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                db.collection("usuarios").document(user.uid)
                    .update("puntuacion", 0).await()
                _puntosTotales.value = 0
                actualizarNombreNivel(0)
                Log.d("Firebase", "Puntos reseteados con éxito")
            } catch (e: Exception) {
                Log.e("Firebase", "Error al resetear: ${e.message}")
            }
        }
    }

    fun cargarPerfil() {
        val user = auth.currentUser
        if (user == null) {
            _nivelActual.value = "Modo Invitado 🕵️"
            _puntosTotales.value = 0
            return
        }

        viewModelScope.launch {
            try {
                val document = db.collection("usuarios").document(user.uid).get().await()
                if (document.exists()) {
                    val puntos = document.getLong("puntuacion")?.toInt() ?: 0
                    _puntosTotales.value = puntos
                    actualizarNombreNivel(puntos)
                } else {
                    db.collection("usuarios").document(user.uid).set(mapOf("puntuacion" to 0)).await()
                    actualizarNombreNivel(0)
                }
            } catch (e: Exception) {
                Log.e("FirebaseError", "Error al cargar: ${e.message}")
                _nivelActual.value = "Novato (Offline)"
            }
        }
    }

    fun sumarPuntos(puntosGanados: Int) {
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                val userRef = db.collection("usuarios").document(user.uid)
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(userRef)
                    val puntosActuales = snapshot.getLong("puntuacion") ?: 0
                    val nuevosPuntos = puntosActuales + puntosGanados
                    transaction.update(userRef, "puntuacion", nuevosPuntos)
                    nuevosPuntos
                }.addOnSuccessListener { puntosActualizados ->
                    _puntosTotales.value = (puntosActualizados as Long).toInt()
                    actualizarNombreNivel(_puntosTotales.value)
                }
            } catch (e: Exception) {
                Log.e("FirebaseError", "Error en transacción: ${e.message}")
            }
        }
    }

    private fun actualizarNombreNivel(puntos: Int) {
        val nivelEncontrado = listaNiveles.findLast { puntos >= it.puntosMinimos }
        _nivelActual.value = nivelEncontrado?.nombre ?: "Novato 🚶"
    }
}