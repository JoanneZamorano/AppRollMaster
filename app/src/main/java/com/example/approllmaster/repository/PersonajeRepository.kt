package com.example.approllmaster.repository

import com.example.approllmaster.models.Guerrero
import com.example.approllmaster.models.Mago
import com.example.approllmaster.models.Personaje
import com.google.firebase.firestore.FirebaseFirestore

class PersonajeRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val coleccionPersonajes = db.collection("personajes")

    //guarda un personaje nuevo o actualiza el que ya existe
    fun guardarPersonaje(
        personaje: Personaje,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        //si todavia no tiene id en Firebase, dejamos que cree uno nuevo
        val docRef = if (personaje.idPersonaje.isEmpty()) {
            coleccionPersonajes.document()
        } else {
            coleccionPersonajes.document(personaje.idPersonaje)
        }

        // le mete el id generado a la ficha antes de guardarla
        val datosAGuardar = when (personaje) {
            is Guerrero -> personaje.copy(idPersonaje = docRef.id)
            is Mago -> personaje.copy(idPersonaje = docRef.id)
            else -> personaje
        }

        docRef.set(datosAGuardar)
            .addOnSuccessListener {
                onSuccess(docRef.id)
            }
            .addOnFailureListener { error ->
                onError("No se pudo guardar la ficha: ${error.message}")
            }
    }

    // trae todas las fichas que ha creado un usuario
    fun obtenerPersonajesDeUsuario(
        idUsuario: String,
        onSuccess: (List<Personaje>) -> Unit,
        onError: (String) -> Unit
    ) {
        coleccionPersonajes.whereEqualTo("idUsuario", idUsuario)
            .get()
            .addOnSuccessListener { snapshot ->
                val lista = mutableListOf<Personaje>()
                for (doc in snapshot.documents) {
                    val clase = doc.getString("clase") ?: ""
                    // Miramos si es guerrero o mago para convertirlo a su clase
                    val personaje = if (clase == "GUERRERO") {
                        doc.toObject(Guerrero::class.java)
                    } else {
                        doc.toObject(Mago::class.java)
                    }
                    if (personaje != null) {
                        lista.add(personaje)
                    }
                }
                onSuccess(lista)
            }
            .addOnFailureListener { error ->
                onError("Error al cargar personajes: ${error.message}")
            }
    }
}