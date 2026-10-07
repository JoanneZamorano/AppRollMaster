package com.example.approllmaster.repository

import com.example.approllmaster.models.Usuario
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    //para saber si ya hay alguien logueado en la app
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    // crea la cuenta en firebase y luego guarda el perfil en la base de datos
    fun registrarUsuario(
        email: String,
        pass: String,
        nombreUsuario: String,
        onSuccess: (Usuario) -> Unit,
        onError: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid ?: ""
                val nuevoUsuario = Usuario(
                    uid = uid,
                    email = email,
                    nombreUsuario = nombreUsuario
                )

                // Guardamos sus datos con el mismo ID que le dio Firebase al registrarse
                db.collection("usuarios").document(uid).set(nuevoUsuario)
                    .addOnSuccessListener {
                        onSuccess(nuevoUsuario)
                    }
                    .addOnFailureListener { error ->
                        onError("No se pudo guardar el usuario: ${error.message}")
                    }
            }
            .addOnFailureListener { error ->
                onError("Error al registrarse: ${error.message}")
            }
    }

    // iniciar sesion -> con correo y contraseña
    fun iniciarSesion(
        email: String,
        pass: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { error ->
                onError("Error al entrar: ${error.message}")
            }
    }

    // desconectar la cuenta actual
    fun cerrarSesion() {
        auth.signOut()
    }
}