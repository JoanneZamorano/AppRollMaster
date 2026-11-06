package com.example.approllmaster

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.approllmaster.databinding.ActivityJoinGameBinding
import com.example.approllmaster.models.Personaje
import com.example.approllmaster.repository.AuthRepository
import com.google.firebase.firestore.FirebaseFirestore

class JoinGameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityJoinGameBinding
    private val authRepository = AuthRepository()
    private val db = FirebaseFirestore.getInstance()

    // Lista en memoria de los personajes del jugador
    private val misPersonajes = mutableListOf<Personaje>()
    private var personajeSeleccionado: Personaje? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJoinGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cargarPersonajesDelUsuario()

        binding.btnVolverMenuDesdeUnirse.setOnClickListener {
            finish()
        }

        binding.btnUnirseSala.setOnClickListener {
            unirseAPartida()
        }
    }

    private fun cargarPersonajesDelUsuario() {
        val userId = authRepository.currentUser?.uid ?: return

        db.collection("personajes")
            .whereEqualTo("idUsuario", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                misPersonajes.clear()
                val nombres = mutableListOf<String>()

                for (doc in snapshot.documents) {
                    val p = doc.toObject(Personaje::class.java)
                    if (p != null) {
                        misPersonajes.add(p)
                        nombres.add("${p.nombre} (${p.clase})")
                    }
                }

                val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, nombres)
                binding.actvPersonaje.setAdapter(adapter)

                binding.actvPersonaje.setOnItemClickListener { _, _, position, _ ->
                    personajeSeleccionado = misPersonajes[position]
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error cargando personajes: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun unirseAPartida() {
        val codigo = binding.etCodigoSala.text.toString().trim().uppercase()

        if (codigo.isEmpty()) {
            binding.tilCodigoSala.error = "Introduce el código de sala"
            return
        }
        binding.tilCodigoSala.error = null

        if (personajeSeleccionado == null) {
            binding.tilSeleccionarPersonaje.error = "Selecciona un personaje"
            return
        }
        binding.tilSeleccionarPersonaje.error = null

        mostrarCarga(true)

        // Buscar si existe una partida con ese código
        db.collection("partidas")
            .whereEqualTo("codigoSala", codigo)
            .whereEqualTo("estado", "ESPERA")
            .get()
            .addOnSuccessListener { snapshot ->
                mostrarCarga(false)
                if (!snapshot.isEmpty) {
                    val docPartida = snapshot.documents[0]
                    val idPartida = docPartida.id

                    Toast.makeText(
                        this,
                        "¡Unido a la partida con ${personajeSeleccionado?.nombre}!",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Próximo paso: pasar a la pantalla de Mesa de Combate
                } else {
                    binding.tilCodigoSala.error = "Código no encontrado o sala cerrada"
                }
            }
            .addOnFailureListener { e ->
                mostrarCarga(false)
                Toast.makeText(this, "Error al buscar sala: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun mostrarCarga(cargando: Boolean) {
        binding.progressBarUnirse.visibility = if (cargando) View.VISIBLE else View.GONE
        binding.btnUnirseSala.isEnabled = !cargando
    }
}