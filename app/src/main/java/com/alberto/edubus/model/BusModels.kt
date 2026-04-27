package com.alberto.edubus.model

import com.google.gson.annotations.SerializedName
import java.time.DayOfWeek
import java.time.LocalDate

data class RutaBus(
    @SerializedName("linea_id") val lineaId: String,
    @SerializedName("nombre_comercial") val nombreComercial: String,
    @SerializedName("id_surbus_filtro") val idSurbusFiltro: String,

    // Horarios Laborables
    @SerializedName("horarios_salida_ida_laborables") val horariosIdaLaborables: List<String>?,
    @SerializedName("horarios_salida_vuelta_laborables") val horariosVueltaLaborables: List<String>?,

    // Horarios Sábados
    @SerializedName("horarios_salida_ida_sabados") val horariosIdaSabados: List<String>?,
    @SerializedName("horarios_salida_vuelta_sabados") val horariosVueltaSabados: List<String>?,

    // Horarios Domingos
    @SerializedName("horarios_salida_ida_domingos") val horariosIdaDomingos: List<String>?,
    @SerializedName("horarios_salida_vuelta_domingos") val horariosVueltaDomingos: List<String>?,

    @SerializedName("paradas_ida") val paradasIda: List<Parada>,
    @SerializedName("paradas_vuelta") val paradasVuelta: List<Parada>
) {
    // Esta función devuelve la lista correcta dependiendo del día de la semana
    fun obtenerHorariosActuales(esIda: Boolean): List<String>? {
        val hoy = LocalDate.now().dayOfWeek
        return when (hoy) {
            DayOfWeek.SUNDAY -> if (esIda) horariosIdaDomingos else horariosVueltaDomingos
            DayOfWeek.SATURDAY -> if (esIda) horariosIdaSabados else horariosVueltaSabados
            else -> if (esIda) horariosIdaLaborables else horariosVueltaLaborables
        }
    }
}

data class Parada(
    @SerializedName("orden") val orden: Int,
    @SerializedName("id_parada") val idParada: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("latitud") val latitud: Double?,
    @SerializedName("longitud") val longitud: Double?,
    @SerializedName("tiempo_desde_inicio_min") val tiempoAcumulado: Int
)