package com.example.approllmaster.models

open class Personaje(
    open val idPersonaje: String = "",
    open val idUsuario: String = "",
    open val nombre: String = "",
    open val clase: String = "", // "GUERRERO" o "MAGO"
    open val saludMax: Int = 100,
    open var saludActual: Int = 100,
    open val avatarUrl: String = ""
)

data class Guerrero(
    override val idPersonaje: String = "",
    override val idUsuario: String = "",
    override val nombre: String = "",
    override val clase: String = "GUERRERO",
    override val saludMax: Int = 120,
    override var saludActual: Int = 120,
    override val avatarUrl: String = "",
    val furiaMax: Int = 100,
    var furiaActual: Int = 0
) : Personaje(idPersonaje, idUsuario, nombre, clase, saludMax, saludActual, avatarUrl)

data class Mago(
    override val idPersonaje: String = "",
    override val idUsuario: String = "",
    override val nombre: String = "",
    override val clase: String = "MAGO",
    override val saludMax: Int = 80,
    override var saludActual: Int = 80,
    override val avatarUrl: String = "",
    val manaMax: Int = 100,
    var manaActual: Int = 100
) : Personaje(idPersonaje, idUsuario, nombre, clase, saludMax, saludActual, avatarUrl)