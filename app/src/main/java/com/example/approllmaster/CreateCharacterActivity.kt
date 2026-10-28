package com.example.approllmaster

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.approllmaster.databinding.ActivityCreateCharacterBinding
import com.example.approllmaster.models.Guerrero
import com.example.approllmaster.models.Mago
import com.example.approllmaster.repository.AuthRepository
import com.google.firebase.firestore.FirebaseFirestore

class CreateCharacterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateCharacterBinding
    private val authRepository = AuthRepository()
    private val db = FirebaseFirestore.getInstance()

    private val clases = listOf("Guerrero", "Mago")
    private val avatares = listOf("Guerrero Valiente", "Mago Arcano", "Explorador", "Paladín")

    private val avatarUrls = mapOf(
        "Guerrero Valiente" to "https://api.dicebear.com/7.x/bottts/png?seed=GuerreroValiente",
        "Mago Arcano" to "https://api.dicebear.com/7.x/bottts/png?seed=MagoArcano",
        "Explorador" to "https://api.dicebear.com/7.x/bottts/png?seed=Explorador",
        "Paladín" to "https://api.dicebear.com/7.x/bottts/png?seed=Paladin"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateCharacterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapterClases = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, clases)
        binding.actvClase.setAdapter(adapterClases)

        val adapterAvatares = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, avatares)
        binding.actvAvatar.setAdapter(adapterAvatares)

        binding.actvClase.setOnItemClickListener { _, _, position, _ ->
            if (clases[position] == "Guerrero") {
                binding.tilRecurso.hint = "Furia máxima"
                binding.etSalud.setText("120")
                binding.etRecurso.setText("100")
                binding.actvAvatar.setText("Guerrero Valiente", false)
            } else {
                binding.tilRecurso.hint = "Maná máximo"
                binding.etSalud.setText("80")
                binding.etRecurso.setText("100")
                binding.actvAvatar.setText("Mago Arcano", false)
            }
        }

        binding.btnCancelarCrear.setOnClickListener {
            finish()
        }

        binding.btnGuardarPersonaje.setOnClickListener {
            guardarPersonajeEnFirestore()
        }
    }

    private fun guardarPersonajeEnFirestore() {
        val nombre = binding.etNombrePersonaje.text.toString().trim()
        val claseSeleccionada = binding.actvClase.text.toString().trim()
        val avatarSeleccionado = binding.actvAvatar.text.toString().trim()
        val saludStr = binding.etSalud.text.toString().trim()
        val recursoStr = binding.etRecurso.text.toString().trim()

        if (nombre.isEmpty()) {
            binding.tilNombrePersonaje.error = "Introduce un nombre para el personaje"
            return
        }
        binding.tilNombrePersonaje.error = null

        val salud = saludStr.toIntOrNull() ?: 100
        val recurso = recursoStr.toIntOrNull() ?: 100
        val userId = authRepository.currentUser?.uid ?: return

        val nuevoDoc = db.collection("personajes").document()
        val personajeId = nuevoDoc.id
        val urlFinal = avatarUrls[avatarSeleccionado] ?: "https://api.dicebear.com/7.x/bottts/png?seed=$nombre"

        val personajeObj = if (claseSeleccionada == "Guerrero") {
            Guerrero(
                idPersonaje = personajeId,
                idUsuario = userId,
                nombre = nombre,
                clase = "GUERRERO",
                saludMax = salud,
                saludActual = salud,
                avatarUrl = urlFinal,
                furiaMax = recurso,
                furiaActual = 0
            )
        } else {
            Mago(
                idPersonaje = personajeId,
                idUsuario = userId,
                nombre = nombre,
                clase = "MAGO",
                saludMax = salud,
                saludActual = salud,
                avatarUrl = urlFinal,
                manaMax = recurso,
                manaActual = recurso
            )
        }

        mostrarCarga(true)

        nuevoDoc.set(personajeObj)
            .addOnSuccessListener {
                mostrarCarga(false)
                Toast.makeText(
                    this, "¡Personaje creado correctamente!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, CharacterListActivity::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                mostrarCarga(false)
                Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun mostrarCarga(cargando: Boolean) {
        binding.progressBarCrear.visibility = if (cargando) View.VISIBLE else View.GONE
        binding.btnGuardarPersonaje.isEnabled = !cargando
    }
}