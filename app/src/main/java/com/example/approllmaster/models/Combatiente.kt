package com.example.approllmaster.models

data class Combatiente(
    var id: String = "",
    var idUsuario: String = "",
    var nombre: String = "",
    var tipo: String = "JUGADOR",
    var claseOEstado: String = "",
    var psActuales: Int = 0,
    var psMaximos: Int = 0,
    var ca: Int = 10,
    var bonoAtaque: Int = 0,
    var ordenTurno: Int = 0,
    var avatarUrl: String = ""
)