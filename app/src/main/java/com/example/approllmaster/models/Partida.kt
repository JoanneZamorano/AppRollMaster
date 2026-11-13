package com.example.approllmaster.models

data class Partida(
    val idPartida: String = "",
    val nombrePartida: String = "",
    val codigoSala: String = "", // por ejemplo ROLL84
    val idMaster: String = "",
    val estado: String = "ESPERA", // ESPERA/EN_COMBATE/FINALIZADA
    val fechaCreacion: Long = System.currentTimeMillis()
)


data class TiradaDado(
    val idTirada: String = "",
    val remitente: String = "",
    val valor: Int = 0,               // 1 al 20
    val esCritico: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)