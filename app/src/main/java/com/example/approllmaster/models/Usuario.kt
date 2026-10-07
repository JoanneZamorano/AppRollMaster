package com.example.approllmaster.models

data class Usuario(
    val uid: String = "",
    val email: String = "",
    val nombreUsuario: String = "",
    val fechaRegistro: Long = System.currentTimeMillis()
)