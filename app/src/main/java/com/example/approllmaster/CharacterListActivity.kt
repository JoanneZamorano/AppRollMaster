package com.example.approllmaster

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.approllmaster.databinding.ActivityCharacterListBinding
import com.example.approllmaster.models.Guerrero
import com.example.approllmaster.models.Mago
import com.example.approllmaster.models.Personaje
import com.example.approllmaster.repository.AuthRepository
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