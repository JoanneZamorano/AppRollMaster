package com.example.approllmaster

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.approllmaster.databinding.ActivityJoinGameBinding
import com.example.approllmaster.models.Personaje
import com.example.approllmaster.repository.AuthRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class JoinGameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityJoinGameBinding
    private val authRepository = AuthRepository()
    private val db = FirebaseFirestore.getInstance()

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

        db.collection("partidas")
            .whereEqualTo("codigoSala", codigo)
            .whereEqualTo("estado", "ESPERA")
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val docPartida = snapshot.documents[0]
                    val idPartida = docPartida.id
                    val nombrePartida = docPartida.getString("nombrePartida") ?: "Partida"
                    val heroe = personajeSeleccionado!!
                    val uidJugador = authRepository.currentUser?.uid ?: ""
                    val esGuerrero = heroe.clase.uppercase() == "GUERRERO"

                    val datosHeroe = hashMapOf<String, Any>(
                        "id" to (heroe.idPersonaje.ifEmpty { uidJugador }),
                        "idUsuario" to uidJugador,
                        "nombre" to heroe.nombre,
                        "tipo" to "JUGADOR",
                        "claseOEstado" to heroe.clase,
                        "psActuales" to heroe.saludMax,
                        "psMaximos" to heroe.saludMax,
                        "ca" to (if (esGuerrero) 15 else 12),
                        "bonoAtaque" to (if (esGuerrero) 4 else 5),
                        "ordenTurno" to 0,
                        "avatarUrl" to heroe.avatarUrl
                    )

                    db.collection("partidas").document(idPartida)
                        .update(
                            "combatientes", FieldValue.arrayUnion(datosHeroe),
                            "ultimoLog", "¡${heroe.nombre} (${heroe.clase}) se ha unido a la sala!"
                        )
                        .addOnSuccessListener {
                            mostrarCarga(false)
                            val intent = Intent(this, CombatTableActivity::class.java).apply {
                                putExtra("ID_PARTIDA", idPartida)
                                putExtra("CODIGO_SALA", codigo)
                                putExtra("NOMBRE_PARTIDA", nombrePartida)
                                putExtra("ROL_USUARIO", "JUGADOR")
                            }
                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            mostrarCarga(false)
                            Toast.makeText(this, "Error al entrar: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    mostrarCarga(false)
                    binding.tilCodigoSala.error = "Código no encontrado o combate ya iniciado"
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