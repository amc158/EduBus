package com.alberto.edubus.model

import com.google.gson.annotations.SerializedName

data class RutaBus(
    @SerializedName("linea_id") val lineaId: String,
    @SerializedName("nombre_comercial") val nombreComercial: String,
    @SerializedName("id_surbus_filtro") val idSurbusFiltro: String,
    @SerializedName("paradas_ida") val paradasIda: List<Parada>,
    @SerializedName("paradas_vuelta") val paradasVuelta: List<Parada>
)

data class Parada(
    @SerializedName("orden") val orden: Int,
    @SerializedName("id_parada") val idParada: String,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("latitud") val latitud: Double,
    @SerializedName("longitud") val longitud: Double,
    @SerializedName("tiempo_desde_inicio_min") val tiempoAcumulado: Int
)