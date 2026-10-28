package com.example.approllmaster

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.approllmaster.databinding.ActivityMainBinding
import com.example.approllmaster.repository.AuthRepository
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val authRepository = AuthRepository()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //cmprobar si hay un usuario conectado
        val usuarioActual = authRepository.currentUser
        if (usuarioActual != null) {

            //consulta el nombre en Firestore
            db.collection("usuarios").document(usuarioActual.uid)
                .get()
                .addOnSuccessListener { doc ->
                    val nombre = doc.getString("nombreUsuario") ?: "Aventurero"
                    binding.tvBienvenida.text = "¡Saludos, $nombre!"
                }
        }

        // Cerrar sesion
        binding.btnCerrarSesion.setOnClickListener {
            authRepository.cerrarSesion()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        // 1- mis personajes
        binding.btnMisPersonajes.setOnClickListener {
            Toast.makeText(this, "Próximamente: lista de personajes", Toast.LENGTH_SHORT).show()
        }

        // 2 - crear NUEVO personaje -> Abre el formulario directamente
        binding.btnCrearPersonajeMenu.setOnClickListener {
            val intent = Intent(this, CreateCharacterActivity::class.java)
            startActivity(intent)
        }

        // 3 -crear partida (Master)
        binding.btnCrearPartida.setOnClickListener {
            Toast.makeText(this, "Próximamente: crear sala de partida", Toast.LENGTH_SHORT).show()
        }

        // 4- unirse a Partida (Jugador)
        binding.btnUnirsePartida.setOnClickListener {
            Toast.makeText(this, "Próximamente: unirse con código PIN", Toast.LENGTH_SHORT).show()
        }
    }
}