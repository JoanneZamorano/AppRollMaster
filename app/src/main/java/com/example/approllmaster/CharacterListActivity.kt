package com.example.approllmaster

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.approllmaster.databinding.ActivityCharacterListBinding
import com.example.approllmaster.models.Guerrero
import com.example.approllmaster.models.Mago
import com.example.approllmaster.models.Personaje
import com.example.approllmaster.repository.AuthRepository
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore

class CharacterListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCharacterListBinding
    private val authRepository = AuthRepository()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: PersonajeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCharacterListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = PersonajeAdapter(
            onEditarClick = { personaje -> abrirDialogoEditarPersonaje(personaje) },
            onEliminarClick = { personaje -> confirmarEliminacion(personaje) }
        )

        // Configurar RecyclerView
        binding.rvPersonajes.layoutManager = LinearLayoutManager(this)
        binding.rvPersonajes.adapter = adapter

        // Botón inferior para crear nuevo personaje
        binding.btnCrearPersonajeDesdeLista.setOnClickListener {
            val intent = Intent(this, CreateCharacterActivity::class.java)
            startActivity(intent)
        }

        // Botón inferior para volver al menú principal
        binding.btnVolverMenu.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        cargarPersonajes()
    }

    private fun cargarPersonajes() {
        val userId = authRepository.currentUser?.uid ?: return

        binding.progressBarLista.visibility = View.VISIBLE
        binding.tvSinPersonajes.visibility = View.GONE

        db.collection("personajes")
            .whereEqualTo("idUsuario", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                binding.progressBarLista.visibility = View.GONE
                val personajes = mutableListOf<Personaje>()

                for (doc in snapshot.documents) {
                    val clase = doc.getString("clase") ?: ""
                    val p = if (clase == "GUERRERO") {
                        doc.toObject(Guerrero::class.java)
                    } else {
                        doc.toObject(Mago::class.java)
                    }
                    if (p != null) {
                        personajes.add(p)
                    }
                }

                adapter.actualizarLista(personajes)
                binding.tvSinPersonajes.visibility =
                    if (personajes.isEmpty()) View.VISIBLE else View.GONE
            }
            .addOnFailureListener { e ->
                binding.progressBarLista.visibility = View.GONE
                Toast.makeText(this, "Error al cargar personajes: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun abrirDialogoEditarPersonaje(personaje: Personaje) {
        if (personaje.idPersonaje.isBlank()) {
            Toast.makeText(this, "No se pudo identificar el personaje", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_editar_personaje, null)
        val tvClase = dialogView.findViewById<TextView>(R.id.tvSubtituloEditarClase)
        val tilNombre = dialogView.findViewById<TextInputLayout>(R.id.tilEditNombre)
        val etNombre = dialogView.findViewById<TextInputEditText>(R.id.etEditNombre)
        val etSaludActual = dialogView.findViewById<TextInputEditText>(R.id.etEditSaludActual)
        val etSaludMax = dialogView.findViewById<TextInputEditText>(R.id.etEditSaludMax)
        val tvLabelRecurso = dialogView.findViewById<TextView>(R.id.tvLabelEditRecurso)
        val tilRecursoActual = dialogView.findViewById<TextInputLayout>(R.id.tilEditRecursoActual)
        val etRecursoActual = dialogView.findViewById<TextInputEditText>(R.id.etEditRecursoActual)
        val tilRecursoMax = dialogView.findViewById<TextInputLayout>(R.id.tilEditRecursoMax)
        val etRecursoMax = dialogView.findViewById<TextInputEditText>(R.id.etEditRecursoMax)
        val btnGuardar = dialogView.findViewById<Button>(R.id.btnGuardarEdicionPersonaje)
        val btnCancelar = dialogView.findViewById<Button>(R.id.btnCancelarEdicionPersonaje)

        tvClase.text = personaje.clase
        etNombre.setText(personaje.nombre)
        etSaludActual.setText(personaje.saludActual.toString())
        etSaludMax.setText(personaje.saludMax.toString())

        when (personaje) {
            is Guerrero -> {
                tvLabelRecurso.text = "FURIA"
                tilRecursoActual.hint = "Furia actual"
                tilRecursoMax.hint = "Furia máxima"
                etRecursoActual.setText(personaje.furiaActual.toString())
                etRecursoMax.setText(personaje.furiaMax.toString())
            }
            is Mago -> {
                tvLabelRecurso.text = "MANÁ"
                tilRecursoActual.hint = "Maná actual"
                tilRecursoMax.hint = "Maná máximo"
                etRecursoActual.setText(personaje.manaActual.toString())
                etRecursoMax.setText(personaje.manaMax.toString())
            }
            else -> {
                etRecursoActual.setText("100")
                etRecursoMax.setText("100")
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        btnGuardar.setOnClickListener {
            val nuevoNombre = etNombre.text.toString().trim()
            if (nuevoNombre.isEmpty()) {
                tilNombre.error = "El nombre no puede estar vacío"
                return@setOnClickListener
            }
            tilNombre.error = null

            val saludMax = (etSaludMax.text.toString().toIntOrNull() ?: personaje.saludMax).coerceAtLeast(1)
            val saludActual = (etSaludActual.text.toString().toIntOrNull() ?: personaje.saludActual).coerceIn(0, saludMax)

            val recursoMax = (etRecursoMax.text.toString().toIntOrNull() ?: 100).coerceAtLeast(1)
            val recursoActual = (etRecursoActual.text.toString().toIntOrNull() ?: 100).coerceIn(0, recursoMax)

            val actualizaciones = mutableMapOf<String, Any>(
                "nombre" to nuevoNombre,
                "saludActual" to saludActual,
                "saludMax" to saludMax
            )

            when (personaje) {
                is Guerrero -> {
                    actualizaciones["furiaActual"] = recursoActual
                    actualizaciones["furiaMax"] = recursoMax
                }
                is Mago -> {
                    actualizaciones["manaActual"] = recursoActual
                    actualizaciones["manaMax"] = recursoMax
                }
            }

            btnGuardar.isEnabled = false
            db.collection("personajes").document(personaje.idPersonaje)
                .update(actualizaciones)
                .addOnSuccessListener {
                    Toast.makeText(this, "$nuevoNombre actualizado", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    cargarPersonajes()
                }
                .addOnFailureListener { e ->
                    btnGuardar.isEnabled = true
                    Toast.makeText(this, "Error al guardar cambios: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        dialog.show()
    }

    private fun confirmarEliminacion(personaje: Personaje) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar personaje")
            .setMessage("¿Seguro que quieres eliminar a ${personaje.nombre}? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarPersonaje(personaje)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarPersonaje(personaje: Personaje) {
        if (personaje.idPersonaje.isBlank()) {
            Toast.makeText(this, "No se pudo identificar el personaje", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBarLista.visibility = View.VISIBLE
        db.collection("personajes").document(personaje.idPersonaje)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "${personaje.nombre} eliminado", Toast.LENGTH_SHORT).show()
                cargarPersonajes()
            }
            .addOnFailureListener { e ->
                binding.progressBarLista.visibility = View.GONE
                Toast.makeText(this, "Error al eliminar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}