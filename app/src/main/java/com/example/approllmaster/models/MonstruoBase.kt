package com.example.approllmaster.models

data class MonstruoBase(
    val id: String = "",
    val nombre: String = "",
    val vida: Int = 10,
    val armadura: Int = 10,
    val ataque: String = "+2",
    val esJefe: Boolean = false,
    var cantidadSeleccionada: Int = 0,
    val avatarUrl: String = ""
)